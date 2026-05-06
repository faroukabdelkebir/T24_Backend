package wevioo.se.authms.UserMS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import wevioo.se.authms.Exceptions.UserServiceException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;

    public record UserCredentials(
            UUID    id,
            String  username,
            String  email,
            String  passwordHash,
            String  role,
            boolean active
    ) {}

    public record PasswordUpdateRequest(String passwordHash) {}

    public Optional<UserCredentials> findByUsername(String username) {
        try {
            UserCredentials creds = restClient.get()
                    .uri("/api/v1/users/internal/credentials?username={u}", username)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                    .body(UserCredentials.class);
            return Optional.ofNullable(creds);
        } catch (Exception ex) {
            log.error("UserMS call failed [findByUsername={}]: {}", username, ex.getMessage());
            throw new UserServiceException("User service unavailable. Please try again later.");
        }
    }

    public Optional<UserCredentials> findByEmail(String email) {
        try {
            UserCredentials creds = restClient.get()
                    .uri("/api/v1/users/internal/credentials?email={e}", email)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                    .body(UserCredentials.class);
            return Optional.ofNullable(creds);
        } catch (Exception ex) {
            log.error("UserMS call failed [findByEmail={}]: {}", email, ex.getMessage());
            throw new UserServiceException("User service unavailable. Please try again later.");
        }
    }

    public Optional<UserCredentials> findById(UUID userId) {
        try {
            UserCredentials creds = restClient.get()
                    .uri("/api/v1/users/internal/credentials/{id}", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {})
                    .body(UserCredentials.class);
            return Optional.ofNullable(creds);
        } catch (Exception ex) {
            log.error("UserMS call failed [findById={}]: {}", userId, ex.getMessage());
            throw new UserServiceException("User service unavailable. Please try again later.");
        }
    }

    public void updatePassword(UUID userId, String newHashedPassword) {
        try {
            restClient.patch()
                    .uri("/api/v1/users/internal/{id}/password", userId)
                    .body(new PasswordUpdateRequest(newHashedPassword))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("UserMS updatePassword failed [userId={}]: {}", userId, ex.getMessage());
            throw new UserServiceException("User service unavailable. Please try again later.");
        }
    }
}

