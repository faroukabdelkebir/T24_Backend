package wevioo.se.userms.Repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wevioo.se.userms.Entities.Role;
import wevioo.se.userms.Entities.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByActive(boolean active, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:role   IS NULL OR u.role   = :role)
          AND (:active IS NULL OR u.active = :active)
        """)
    Page<User> searchUsers(
            @Param("search") String search,
            @Param("role")   Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );


    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :id")
    void updateLastLoginAt(@Param("id") UUID id, @Param("loginTime") Instant loginTime);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveByRole(@Param("role") Role role);
}

