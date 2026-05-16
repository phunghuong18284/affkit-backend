package vn.affkit.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Auth
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Tài khoản bị khóa do nhập sai mật khẩu nhiều lần", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED("ACCOUNT_NOT_VERIFIED", "Vui lòng xác nhận email trước khi đăng nhập", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "Refresh token không hợp lệ hoặc đã bị revoke", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "Refresh token đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("TOKEN_INVALID", "Token không hợp lệ", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token đã hết hạn", HttpStatus.BAD_REQUEST),
    TOKEN_ALREADY_USED("TOKEN_ALREADY_USED", "Token đã được sử dụng", HttpStatus.BAD_REQUEST),

    // User
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    WRONG_PASSWORD("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),

    // Link errors
    LINK_NOT_FOUND("LINK_NOT_FOUND", "Link không tồn tại", HttpStatus.NOT_FOUND),
    LINK_DELETED("LINK_DELETED", "Link đã bị xóa", HttpStatus.GONE),
    LINK_LIMIT_EXCEEDED("LINK_LIMIT_EXCEEDED", "Đã đạt giới hạn 30 link của plan Free", HttpStatus.FORBIDDEN),

    // Campaign errors
    CAMPAIGN_NOT_FOUND("CAMPAIGN_NOT_FOUND", "Campaign không tồn tại", HttpStatus.NOT_FOUND),

    // AccessTrade errors
    ACCESSTRADE_KEY_NOT_FOUND("ACCESSTRADE_KEY_NOT_FOUND", "Chưa kết nối AccessTrade. Vào Cài đặt để nhập API key.", HttpStatus.BAD_REQUEST),
    ACCESSTRADE_API_ERROR("ACCESSTRADE_API_ERROR", "Không thể kết nối AccessTrade API. Kiểm tra lại API key.", HttpStatus.BAD_GATEWAY),

    // Common
    VALIDATION_ERROR("VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY),
    UNAUTHORIZED("UNAUTHORIZED", "Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("INTERNAL_ERROR", "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}