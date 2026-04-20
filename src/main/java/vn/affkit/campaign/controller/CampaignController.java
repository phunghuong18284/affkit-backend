package vn.affkit.campaign.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.affkit.auth.entity.User;
import vn.affkit.campaign.dto.CampaignResponse;
import vn.affkit.campaign.dto.CreateCampaignRequest;
import vn.affkit.campaign.dto.UpdateCampaignRequest;
import vn.affkit.campaign.service.CampaignService;
import vn.affkit.common.ApiResponse;
import vn.affkit.link.dto.LinkResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Quan ly campaign")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @Operation(summary = "Tao campaign moi")
    public ResponseEntity<ApiResponse<CampaignResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCampaignRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campaignService.create(user.getId(), req)));
    }

    @GetMapping
    @Operation(summary = "Danh sach campaign")
    public ResponseEntity<ApiResponse<Page<CampaignResponse>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.list(user.getId(), page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiet campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.getById(user.getId(), id)));
    }

    @GetMapping("/{id}/links")
    @Operation(summary = "Links trong campaign")
    public ResponseEntity<ApiResponse<Page<LinkResponse>>> getLinks(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.getLinks(user.getId(), id, page, size)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cap nhat campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(
                campaignService.update(user.getId(), id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoa campaign (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        campaignService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}