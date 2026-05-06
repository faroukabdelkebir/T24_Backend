package wevioo.se.authms.Services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wevioo.se.authms.Config.AppProperties;
import wevioo.se.authms.Dto.ChangePasswordRequest;
import wevioo.se.authms.Dto.ForgotPasswordRequest;
import wevioo.se.authms.Dto.LoginRequest;
import wevioo.se.authms.Dto.RefreshTokenRequest;
import wevioo.se.authms.Dto.ResetPasswordRequest;
import wevioo.se.authms.Dto.AuthResponse;
import wevioo.se.authms.Dto.MessageResponse;
import wevioo.se.authms.Entities.AuthEventType;
import wevioo.se.authms.Entities.PasswordResetToken;
import wevioo.se.authms.Entities.RefreshToken;
import wevioo.se.authms.Exceptions.*;
import wevioo.se.authms.Repositories.PasswordResetTokenRepository;
import wevioo.se.authms.Repositories.RefreshTokenRepository;
import wevioo.se.authms.UserMS.UserPrincipal;
import wevioo.se.authms.UserMS.UserServiceClient;
import wevioo.se.authms.UserMS.UserServiceClient.UserCredentials;
import wevioo.se.authms.Services.AuditService;
import wevioo.se.authms.Services.AuthService;
import wevioo.se.authms.Services.MailService;
import wevioo.se.authms.Services.Utils.JwtService;
import wevioo.se.authms.Services.Utils.TokenHashUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Slf4j
@Service("authService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserServiceClient            userServiceClient;
    private final RefreshTokenRepository       refreshTokenRepo;
    private final PasswordResetTokenRepository resetTokenRepo;
    private final JwtService                   jwtService;
    private final PasswordEncoder              passwordEncoder;
    private final AuditService                 auditService;
    private final MailService                  mailService;
    private final AppProperties                props;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.debug("Login attempt: username={}", request.username());

        UserCredentials creds = userServiceClient.findByUsername(request.username())
                .orElseThrow(() -> {
                    auditService.log(AuthEventType.LOGIN_FAILURE, request.username(),
                            false, "User not found", httpRequest);
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!creds.active()) {
            auditService.log(AuthEventType.LOGIN_FAILURE, request.username(),
                    false, "Account disabled", httpRequest);
            throw new DisabledException("Account is disabled. Contact your administrator.");
        }

        if (!passwordEncoder.matches(request.password(), creds.passwordHash())) {
            auditService.log(AuthEventType.LOGIN_FAILURE, request.username(),
                    false, "Bad credentials", httpRequest);
            throw new BadCredentialsException("Invalid username or password");
        }

        UserPrincipal principal   = toPrincipal(creds);
        String        accessToken  = jwtService.generateAccessToken(principal);
        String        rawRefresh   = TokenHashUtil.generateRaw();
        String        hashedRefresh = TokenHashUtil.hashToken(rawRefresh);

        refreshTokenRepo.save(RefreshToken.builder()
                .token(hashedRefresh)
                .userId(creds.id())
                .expiresAt(Instant.now().plusMillis(props.getJwt().getRefreshTokenExpirationMs()))
                .build());

        auditService.log(AuthEventType.LOGIN_SUCCESS, request.username(), true,
                "Login successful", httpRequest);

        return AuthResponse.of(accessToken, rawRefresh,
                props.getJwt().getAccessTokenExpirationMs(),
                creds.id(), creds.username(), creds.email(), creds.role());
    }

    @Override
    @Transactional
    public MessageResponse logout(String rawRefreshToken, HttpServletRequest httpRequest) {
        String hashed = TokenHashUtil.hashToken(rawRefreshToken);
        refreshTokenRepo.findByToken(hashed).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
            auditService.log(AuthEventType.LOGOUT, null, true, "Token revoked", httpRequest);
        });
        return MessageResponse.of("Logged out successfully.");
    }

    @Override
    @Transactional
    public MessageResponse logoutAll(UUID userId, HttpServletRequest httpRequest) {
        refreshTokenRepo.revokeAllByUserId(userId);
        auditService.log(AuthEventType.LOGOUT, null, true,
                "All sessions revoked for userId=" + userId, httpRequest);
        return MessageResponse.of("All sessions terminated successfully.");
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String hashed = TokenHashUtil.hashToken(request.refreshToken());

        RefreshToken rt = refreshTokenRepo.findByToken(hashed)
                .orElseThrow(() -> {
                    auditService.log(AuthEventType.TOKEN_REFRESH_FAILURE, null, false,
                            "Token not found", httpRequest);
                    return new InvalidTokenException("Invalid refresh token.");
                });

        if (rt.isRevoked()) {
            log.warn("Refresh token reuse detected! Revoking all sessions for userId={}", rt.getUserId());
            refreshTokenRepo.revokeAllByUserId(rt.getUserId());
            auditService.log(AuthEventType.TOKEN_REFRESH_FAILURE, null, false,
                    "Reuse detected – all sessions revoked", httpRequest);
            throw new InvalidTokenException("Token reuse detected. All sessions have been terminated.");
        }

        if (rt.isExpired()) {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
            throw new TokenExpiredException("Session expired. Please login again.");
        }

        UserCredentials creds = userServiceClient.findById(rt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!creds.active()) {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
            throw new DisabledException("Account has been disabled.");
        }

        rt.setRevoked(true);
        refreshTokenRepo.save(rt);

        String newRaw    = TokenHashUtil.generateRaw();
        String newHashed = TokenHashUtil.hashToken(newRaw);
        refreshTokenRepo.save(RefreshToken.builder()
                .token(newHashed)
                .userId(rt.getUserId())
                .expiresAt(Instant.now().plusMillis(props.getJwt().getRefreshTokenExpirationMs()))
                .build());

        String newAccess = jwtService.generateAccessToken(
                creds.id(), creds.username(), creds.email(), creds.role());

        auditService.log(AuthEventType.TOKEN_REFRESH, creds.username(), true, "Token rotated", httpRequest);

        return AuthResponse.of(newAccess, newRaw,
                props.getJwt().getAccessTokenExpirationMs(),
                creds.id(), creds.username(), creds.email(), creds.role());
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        final String msg = "If this email is registered, you will receive a reset link shortly.";

        userServiceClient.findByEmail(request.email()).ifPresent(creds -> {
            resetTokenRepo.invalidateAllByUserId(creds.id());

            String rawToken    = TokenHashUtil.generateRaw();
            String hashedToken = TokenHashUtil.hashToken(rawToken);
            Instant expiresAt  = Instant.now()
                    .plus(props.getResetPassword().getTokenExpirationMinutes(), ChronoUnit.MINUTES);

            resetTokenRepo.save(PasswordResetToken.builder()
                    .token(hashedToken)
                    .userId(creds.id())
                    .expiresAt(expiresAt)
                    .build());

            String resetLink = props.getResetPassword().getBaseUrl()
                    + "/reset-password?token=" + rawToken;

            mailService.sendPasswordResetEmail(creds.email(), creds.username(), resetLink,
                    props.getResetPassword().getTokenExpirationMinutes());

            auditService.log(AuthEventType.PASSWORD_RESET_REQUEST, creds.username(), true,
                    "Reset email sent", httpRequest);
        });

        return MessageResponse.of(msg);
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("Passwords do not match.");
        }

        String hashed = TokenHashUtil.hashToken(request.token());

        PasswordResetToken prt = resetTokenRepo.findByToken(hashed)
                .orElseThrow(() -> {
                    auditService.log(AuthEventType.PASSWORD_RESET_FAILURE, null, false,
                            "Invalid token", httpRequest);
                    return new InvalidTokenException("Invalid or expired reset link.");
                });

        if (prt.isUsed()) {
            throw new InvalidTokenException("This reset link has already been used.");
        }

        if (prt.isExpired()) {
            prt.setUsed(true);
            resetTokenRepo.save(prt);
            throw new TokenExpiredException("This reset link has expired. Please request a new one.");
        }

        userServiceClient.updatePassword(prt.getUserId(), passwordEncoder.encode(request.newPassword()));

        prt.setUsed(true);
        resetTokenRepo.save(prt);
        refreshTokenRepo.revokeAllByUserId(prt.getUserId());

        auditService.log(AuthEventType.PASSWORD_RESET_SUCCESS, null, true,
                "Password reset for userId=" + prt.getUserId(), httpRequest);

        return MessageResponse.of("Password reset successfully. Please login with your new password.");
    }

    @Override
    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request,
                                          HttpServletRequest httpRequest) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("Passwords do not match.");
        }

        UserCredentials creds = userServiceClient.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(request.currentPassword(), creds.passwordHash())) {
            auditService.log(AuthEventType.PASSWORD_CHANGE, creds.username(), false,
                    "Wrong current password", httpRequest);
            throw new BadCredentialsException("Current password is incorrect.");
        }

        userServiceClient.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        refreshTokenRepo.revokeAllByUserId(userId);
        mailService.sendPasswordChangedNotification(creds.email(), creds.username());

        auditService.log(AuthEventType.PASSWORD_CHANGE, creds.username(), true,
                "Password changed", httpRequest);

        return MessageResponse.of("Password changed successfully. Please login again.");
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepo.deleteExpiredAndRevoked(Instant.now());
        resetTokenRepo.deleteExpiredAndUsed(Instant.now());
        log.info("Scheduled token cleanup completed");
    }

    private UserPrincipal toPrincipal(UserCredentials creds) {
        return UserPrincipal.builder()
                .userId(creds.id())
                .username(creds.username())
                .email(creds.email())
                .password(creds.passwordHash())
                .role(creds.role())
                .active(creds.active())
                .build();
    }
}
