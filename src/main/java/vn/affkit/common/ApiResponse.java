package vn.affkit.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(
            String code,
            String message,
            Object details
    ) {
        public static ErrorDetail of(String code, String message) {
            return new ErrorDetail(code, message, null);
        }

        public static ErrorDetail of(String code, String message, Object details) {
            return new ErrorDetail(code, message, details);
        }
    }
}