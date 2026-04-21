package vn.affkit.link.controller;

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
import vn.affkit.common.ApiResponse;
import vn.affkit.link.dto.CreateLinkRequest;
import vn.affkit.link.dto.LinkResponse;
import vn.affkit.link.dto.UpdateLinkRequest;
import vn.affkit.link.service.LinkService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Tag(name = "Links", description = "Quản lý link affiliate")
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    @Operation(summary = "Tạo link rút gọn mới")
    public ResponseEntity<ApiResponse<LinkResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateLinkRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(linkService.create(user.getId(), req)));
    }

    @GetMapping
    @Operation(summary = "Danh sách link của tôi")
    public ResponseEntity<ApiResponse<Page<LinkResponse>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                linkService.list(user.getId(), platform, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết link")
    public ResponseEntity<ApiResponse<LinkResponse>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(
                linkService.getById(user.getId(), id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật title / tags / campaign")
    public ResponseEntity<ApiResponse<LinkResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLinkRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(
                linkService.update(user.getId(), id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa link (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        linkService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/{id}/affiliate-url")
    @Operation(summary = "Lưu affiliate URL cho link")
    public ResponseEntity<ApiResponse<LinkResponse>> saveAffiliateUrl(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        return ResponseEntity.ok(ApiResponse.ok(
                linkService.saveAffiliateUrl(user.getId(), id, body.get("affiliateUrl"))));
    }
}