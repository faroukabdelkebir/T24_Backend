package wevioo.se.userms.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wevioo.se.userms.Dto.PasswordUpdateRequest;
import wevioo.se.userms.Dto.UserCredentialsResponse;
import wevioo.se.userms.Services.UserService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/credentials")
    public ResponseEntity<UserCredentialsResponse> getCredentials(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        log.debug(">>> INTERNAL HIT - username={}, email={}", username, email);

        if (username != null) {
            return userService.findCredentialsByUsername(username)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if (email != null) {
            return userService.findCredentialsByEmail(email)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/credentials/{id}")
    public ResponseEntity<UserCredentialsResponse> getCredentialsById(@PathVariable UUID id) {
        log.debug(">>> INTERNAL HIT - id={}", id);
        return userService.findCredentialsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable UUID id,
            @RequestBody PasswordUpdateRequest request) {
        log.debug(">>> INTERNAL HIT - updatePassword id={}", id);
        userService.updatePassword(id, request.passwordHash());
        return ResponseEntity.noContent().build();
    }


}
