package vn.affkit.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.affkit.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}