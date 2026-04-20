package vn.affkit.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String message
) {
    public static AuthResponse of(String accessToken, long expiresIn) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, null);
    }

    public static AuthResponse of(String accessToken, long expiresIn, String message) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, message);
    }
}