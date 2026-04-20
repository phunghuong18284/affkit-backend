package vn.affkit.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.affkit.auth.entity.EmailToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {
    Optional<EmailToken> findByTokenAndUsedFalse(UUID token);
}