package vn.affkit.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.affkit.auth.entity.User;
import vn.affkit.auth.repository.UserRepository;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        short attempts = (short) (user.getFailedAttempts() + 1);
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            log.warn("Account locked: {}", user.getEmail());
        }
        userRepository.save(user);
    }
}