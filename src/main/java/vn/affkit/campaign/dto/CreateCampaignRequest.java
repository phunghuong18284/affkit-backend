package vn.affkit.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCampaignRequest(

        @NotBlank(message = "Tên campaign không được để trống")
        @Size(max = 255, message = "Tên tối đa 255 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description
) {}