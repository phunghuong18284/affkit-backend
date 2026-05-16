package vn.affkit.commission;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.affkit.auth.entity.User;
import vn.affkit.common.ApiResponse;
import vn.affkit.commission.dto.CommissionResponse;

@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<CommissionResponse>> getCommissions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {

        CommissionResponse data = commissionService.getTransactions(user, page, limit);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}