package vn.affkit.accesstrade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.affkit.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class AccessTradeController {

    private final AccessTradeService accessTradeService;

    @PostMapping("/convert")
    public ResponseEntity<?> convertLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ConvertLinkRequest request
    ) {
        AccessTradeService.ConvertResult result = accessTradeService.convertLink(request.url());

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok(ConvertLinkResponse.from(result)));
        }

        if (result.status() == AccessTradeService.ConvertResult.Status.UNSUPPORTED) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ApiResponse.ErrorDetail.of(
                            "UNSUPPORTED_PLATFORM",
                            "URL khong duoc ho tro. Chi ho tro Tiki, Lazada, Shopee."
                    ))
            );
        }

        return ResponseEntity.internalServerError().body(
                ApiResponse.fail(ApiResponse.ErrorDetail.of(
                        "CONVERT_FAILED",
                        "Khong the convert link. Vui long thu lai."
                ))
        );
    }

    public record ConvertLinkRequest(String url) {}

    public record ConvertLinkResponse(
            String originalUrl,
            String affiliateUrl,
            String platform
    ) {
        public static ConvertLinkResponse from(AccessTradeService.ConvertResult result) {
            return new ConvertLinkResponse(
                    result.originalUrl(),
                    result.affiliateUrl(),
                    result.platform()
            );
        }
    }
}