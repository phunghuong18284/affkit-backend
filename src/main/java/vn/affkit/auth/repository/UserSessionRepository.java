package vn.affkit.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.affkit.auth.entity.UserSession;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshTokenAndIsRevokedFalse(String refreshToken);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.user.id = :userId AND s.isRevoked = false")
    void revokeAllByUserId(UUID userId);
}