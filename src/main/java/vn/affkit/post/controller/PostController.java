package vn.affkit.post.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.affkit.auth.entity.User;
import vn.affkit.common.ApiResponse;
import vn.affkit.post.dto.GeneratePostRequest;
import vn.affkit.post.dto.GeneratePostResponse;
import vn.affkit.post.entity.PostHistory;
import vn.affkit.post.service.PostService;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GeneratePostResponse>> generate(
            @Valid @RequestBody GeneratePostRequest req
    ) {
        User user = getCurrentUser();
        GeneratePostResponse response = postService.generate(req, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PostHistory>>> history(
            @RequestParam(defaultValue = "0") int page
    ) {
        User user = getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(postService.getHistory(user.getId(), page)));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}