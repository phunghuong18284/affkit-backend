package vn.affkit.post.dto;

public record GeneratePostResponse(
        String productName,
        String productPrice,
        String productImage,
        String linkToShare,
        String postZalo,
        String postFacebook,
        String postTelegram,
        boolean scraped
) {}