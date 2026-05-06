package wevioo.se.authms.Services;

import jakarta.servlet.http.HttpServletRequest;
import wevioo.se.authms.Dto.*;

import java.util.UUID;

public interface AuthService {

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    MessageResponse logout(String refreshToken, HttpServletRequest httpRequest);

    MessageResponse logoutAll(UUID userId, HttpServletRequest httpRequest);

    AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest);

    MessageResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);

    MessageResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest);

    MessageResponse changePassword(UUID userId, ChangePasswordRequest request, HttpServletRequest httpRequest);
}

