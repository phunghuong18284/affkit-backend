package vn.affkit.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.affkit.analytics.dto.AnalyticsOverviewResponse;
import vn.affkit.analytics.dto.LinkAnalyticsResponse;
import vn.affkit.analytics.service.AnalyticsService;
import vn.affkit.auth.entity.User;
import vn.affkit.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @RequestParam(defaultValue = "30d") String period,
            @AuthenticationPrincipal User user
    ) {
        AnalyticsOverviewResponse data = analyticsService.getOverview(
                user.getId().toString(), period
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/links/{id}")
    public ResponseEntity<ApiResponse<LinkAnalyticsResponse>> getLinkAnalytics(
            @PathVariable String id,
            @RequestParam(defaultValue = "30d") String period,
            @AuthenticationPrincipal User user
    ) {
        LinkAnalyticsResponse data = analyticsService.getLinkAnalytics(
                id, user.getId().toString(), period
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}