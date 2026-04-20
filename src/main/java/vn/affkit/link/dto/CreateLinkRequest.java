package vn.affkit.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateLinkRequest(

        @NotBlank(message = "URL không được để trống")
        @Size(max = 2048, message = "URL quá dài")
        String originalUrl,

        @Size(max = 255, message = "Tên link tối đa 255 ký tự")
        String title,

        UUID campaignId,

        @Size(max = 5, message = "Tối đa 5 tags")
        List<@Size(max = 50, message = "Tag tối đa 50 ký tự") String> tags
) {}