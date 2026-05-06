package wevioo.se.authms.Repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wevioo.se.authms.Entities.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String hashedToken);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.userId = :userId AND p.used = false")
    void invalidateAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now OR p.used = true")
    void deleteExpiredAndUsed(@Param("now") Instant now);
}
