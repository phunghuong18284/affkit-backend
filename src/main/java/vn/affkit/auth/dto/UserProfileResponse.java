package vn.affkit.auth.dto;

import vn.affkit.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String plan,
        boolean emailVerified,
        Instant createdAt,
        long linksUsed,      // 👈 thêm
        int linksLimit       // 👈 thêm
) {
    public static UserProfileResponse from(User user, long linksUsed) {
        int linksLimit = switch (user.getPlan()) {
            case "PRO" -> 500;
            case "BUSINESS" -> 99999;
            default -> 10; // FREE
        };
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPlan(),
                user.isVerified(),
                user.getCreatedAt(),
                linksUsed,
                linksLimit
        );
    }
}