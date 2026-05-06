package wevioo.se.authms.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(

        String       accessToken,
        String       refreshToken,
        String       tokenType,
        long         expiresIn,
        Instant      issuedAt,
        UUID         userId,
        String       username,
        String       email,
        String       role

) {
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long   expiresInMs,
            UUID   userId,
            String username,
            String email,
            String role) {

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInMs / 1000)
                .issuedAt(Instant.now())
                .userId(userId)
                .username(username)
                .email(email)
                .role(role)
                .build();
    }
}

