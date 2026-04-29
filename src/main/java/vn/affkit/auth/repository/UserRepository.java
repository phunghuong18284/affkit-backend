package vn.affkit.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.affkit.auth.entity.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Query(value = """
    UPDATE users SET
        failed_attempts = failed_attempts + 1,
        is_locked = CASE WHEN failed_attempts + 1 >= 5 THEN true ELSE false END,
        locked_until = CASE WHEN failed_attempts + 1 >= 5 THEN :lockedUntil\\:\\:timestamptz ELSE NULL\\:\\:timestamptz END
    WHERE id = :id
    """, nativeQuery = true)
    void incrementFailedAttempts(@Param("id") UUID id, @Param("lockedUntil") Instant lockedUntil);
}