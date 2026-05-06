package wevioo.se.userms.Services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import wevioo.se.userms.Dto.*;
import wevioo.se.userms.Entities.Role;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse  getUserById(UUID id);

    UserResponse  getUserByUsername(String username);

    Page<UserSummary> searchUsers(String search, Role role, Boolean active, Pageable pageable);

    UserResponse  updateUser(UUID id, UpdateUserRequest request);

    void          deactivateUser(UUID id);

    void          activateUser(UUID id);

    void          deleteUser(UUID id);

    void          recordLogin(UUID id);

    Optional<UserCredentialsResponse> findCredentialsByUsername(String username);
    Optional<UserCredentialsResponse> findCredentialsByEmail(String email);
    Optional<UserCredentialsResponse> findCredentialsById(UUID id);
    void updatePassword(UUID id, String newHashedPassword);
}

