package wevioo.se.authms.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import wevioo.se.authms.Dto.ChangePasswordRequest;
import wevioo.se.authms.Dto.ForgotPasswordRequest;
import wevioo.se.authms.Dto.LoginRequest;
import wevioo.se.authms.Dto.RefreshTokenRequest;
import wevioo.se.authms.Dto.ResetPasswordRequest;
import wevioo.se.authms.Dto.AuthResponse;
import wevioo.se.authms.Dto.MessageResponse;
import wevioo.se.authms.Entities.AuthAuditLog;
import wevioo.se.authms.Entities.AuthEventType;
import wevioo.se.authms.Repositories.AuthAuditLogRepository;
import wevioo.se.authms.UserMS.UserPrincipal;
import wevioo.se.authms.Services.AuthService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService            authService;
    private final AuthAuditLogRepository auditLogRepo;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login request: username={}", request.username());
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(authService.logout(request.refreshToken(), httpRequest));
    }


    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(authService.logoutAll(principal.getUserId(), httpRequest));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(authService.refresh(request, httpRequest));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(authService.forgotPassword(request, httpRequest));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(authService.resetPassword(request, httpRequest));
    }


    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(
                authService.changePassword(principal.getUserId(), request, httpRequest));
    }


    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(new CurrentUserResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getRole()
        ));
    }


    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuthAuditLog>> getAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) AuthEventType eventType,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AuthAuditLog> logs;

        if (username != null && eventType != null) {
            logs = auditLogRepo.findByUsername(username, pageable);
        } else if (username != null) {
            logs = auditLogRepo.findByUsername(username, pageable);
        } else if (eventType != null) {
            logs = auditLogRepo.findByEventType(eventType, pageable);
        } else {
            logs = auditLogRepo.findAll(pageable);
        }

        return ResponseEntity.ok(logs);
    }



    public record CurrentUserResponse(UUID id, String username, String email, String role) {}
}

