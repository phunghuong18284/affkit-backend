package vn.affkit.accesstrade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTradeService {

    private final AccessTradeClient client;
    private final AccessTradeConfig config;

    public String detectPlatform(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase();
        if (lower.contains("tiki.vn")) return "tiki";
        if (lower.contains("lazada.vn")) return "lazada";
        if (lower.contains("shopee.vn")) return "shopee";
        if (lower.contains("tiktok.com") || lower.contains("vt.tiktok.com")) return "tiktok";
        return null;
    }

    public ConvertResult convertLink(String originalUrl) {
        String platform = detectPlatform(originalUrl);
        if (platform == null) {
            return ConvertResult.unsupported(originalUrl);
        }

        String campaignId = config.getCampaignId(platform);
        if (campaignId == null) {
            return ConvertResult.unsupported(originalUrl);
        }

        String affiliateUrl = client.createAffiliateLink(campaignId, originalUrl);
        if (affiliateUrl == null) {
            return ConvertResult.failed(originalUrl, platform);
        }

        return ConvertResult.success(originalUrl, affiliateUrl, platform);
    }

    public record ConvertResult(
            String originalUrl,
            String affiliateUrl,
            String platform,
            Status status
    ) {
        public enum Status { SUCCESS, UNSUPPORTED, FAILED }

        public static ConvertResult success(String original, String affiliate, String platform) {
            return new ConvertResult(original, affiliate, platform, Status.SUCCESS);
        }

        public static ConvertResult unsupported(String original) {
            return new ConvertResult(original, null, null, Status.UNSUPPORTED);
        }

        public static ConvertResult failed(String original, String platform) {
            return new ConvertResult(original, null, platform, Status.FAILED);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }
}