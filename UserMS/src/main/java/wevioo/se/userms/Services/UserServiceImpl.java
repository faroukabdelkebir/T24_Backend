package wevioo.se.userms.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wevioo.se.userms.Dto.*;
import wevioo.se.userms.Entities.Role;
import wevioo.se.userms.Entities.User;
import wevioo.se.userms.Exceptions.ConflictException;
import wevioo.se.userms.Exceptions.ResourceNotFoundException;
import wevioo.se.userms.Mapper.UserMapper;
import wevioo.se.userms.Repositories.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user: username={}, role={}", request.username(), request.role());

        assertUsernameAvailable(request.username());
        assertEmailAvailable(request.email());

        User user = User.builder()
                .username(request.username())
                .email(request.email().toLowerCase())
                .passwordHash(request.password())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return userMapper.toResponse(saved);
    }


    @Override
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: username=" + username));
        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserSummary> searchUsers(String search, Role role, Boolean active, Pageable pageable) {
        return userRepository.searchUsers(search, role, active, pageable)
                .map(userMapper::toSummary);
    }


    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.debug("Updating user: id={}", id);

        User user = findUserById(id);

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            assertEmailAvailable(request.email());
            user.setEmail(request.email().toLowerCase());
        }
        if (request.password() != null) {
            user.setPasswordHash(request.password());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        User updated = userRepository.save(user);
        log.info("User updated: id={}", updated.getId());
        return userMapper.toResponse(updated);
    }


    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = findUserById(id);
        if (!user.isActive()) {
            log.debug("User already inactive: id={}", id);
            return;
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: id={}", id);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = findUserById(id);
        if (user.isActive()) {
            log.debug("User already active: id={}", id);
            return;
        }
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated: id={}", id);
    }


    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findUserById(id);
        userRepository.delete(user);
        log.info("User permanently deleted: id={}", id);
    }


    @Override
    @Transactional
    public void recordLogin(UUID id) {
        userRepository.updateLastLoginAt(id, Instant.now());
    }


    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
    }

    private void assertUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken: " + username);
        }
    }

    private void assertEmailAvailable(String email) {
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new ConflictException("Email already registered: " + email);
        }
    }






    @Override
    public Optional<UserCredentialsResponse> findCredentialsByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> new UserCredentialsResponse(
                        u.getId(), u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), u.getRole().name(), u.isActive()
                ));
    }

    @Override
    public Optional<UserCredentialsResponse> findCredentialsByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserCredentialsResponse(
                        u.getId(), u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), u.getRole().name(), u.isActive()
                ));
    }

    @Override
    public Optional<UserCredentialsResponse> findCredentialsById(UUID id) {
        return userRepository.findById(id)
                .map(u -> new UserCredentialsResponse(
                        u.getId(), u.getUsername(), u.getEmail(),
                        u.getPasswordHash(), u.getRole().name(), u.isActive()
                ));
    }

    @Override
    public void updatePassword(UUID id, String newHashedPassword) {
        userRepository.findById(id).ifPresent(u -> {
            u.setPasswordHash(newHashedPassword);
            userRepository.save(u);
        });
    }
}


