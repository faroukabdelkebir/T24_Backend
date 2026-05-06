package wevioo.se.authms.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wevioo.se.authms.Entities.AuthAuditLog;
import wevioo.se.authms.Entities.AuthEventType;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {

    Page<AuthAuditLog> findByUsername(String username, Pageable pageable);

    Page<AuthAuditLog> findByEventType(AuthEventType eventType, Pageable pageable);

    @Query("""
        SELECT COUNT(a) FROM AuthAuditLog a
        WHERE a.username  = :username
          AND a.eventType = 'LOGIN_FAILURE'
          AND a.success   = false
          AND a.createdAt >= :since
        """)
    long countFailedLoginAttempts(
            @Param("username") String username,
            @Param("since") Instant since
    );
}