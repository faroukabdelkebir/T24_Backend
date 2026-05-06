package wevioo.se.userms.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import wevioo.se.userms.Dto.*;
import wevioo.se.userms.Entities.Role;
import wevioo.se.userms.Services.UserService;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user:write')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /users – creating user");

        UserResponse created = userService.createUser(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user:read', 'SCOPE_user:write')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_user:read', 'SCOPE_user:write')")
    public ResponseEntity<Page<UserSummary>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(userService.searchUsers(search, role, active, pageable));
    }


    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user:write')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("PATCH /users/{} – updating user", id);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }


    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SCOPE_user:write')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        log.info("PATCH /users/{}/deactivate", id);
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SCOPE_user:write')")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        log.info("PATCH /users/{}/activate", id);
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user:write')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/login-event")
    @PreAuthorize("hasAuthority('SCOPE_batch:execute')")
    public ResponseEntity<Void> recordLoginEvent(@PathVariable UUID id) {
        userService.recordLogin(id);
        return ResponseEntity.noContent().build();
    }







}
