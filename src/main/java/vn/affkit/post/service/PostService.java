package vn.affkit.post.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.affkit.accesstrade.AccessTradeService;
import vn.affkit.post.dto.*;
import vn.affkit.post.entity.PostHistory;
import vn.affkit.post.repository.PostHistoryRepository;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class PostService {

    private final ScraperService scraperService;
    private final PostHistoryRepository postHistoryRepository;
    private final AccessTradeService accessTradeService;

    public PostService(ScraperService scraperService,
                       PostHistoryRepository postHistoryRepository,
                       AccessTradeService accessTradeService) {
        this.scraperService = scraperService;
        this.postHistoryRepository = postHistoryRepository;
        this.accessTradeService = accessTradeService;
    }

    public GeneratePostResponse generate(GeneratePostRequest req, UUID userId) {

        // 1. Scrape thông tin sản phẩm
        ScrapedProduct scraped = scraperService.scrape(req.productUrl());

        // 2. Ưu tiên dữ liệu user tự nhập nếu scrape thiếu
        String name = (req.productName() != null && !req.productName().isBlank())
                ? req.productName()
                : scraped.name();

        long priceRaw;
        if (req.productPrice() != null && !req.productPrice().isBlank()) {
            priceRaw = parsePrice(req.productPrice());
        } else {
            priceRaw = scraped.price();
        }

        String image = scraped.imageUrl();

        // 3. Tên mặc định nếu vẫn thiếu
        if (name.isBlank()) {
            if (req.productUrl().contains("tiktok.com")) {
                name = "Sản phẩm TikTok Shop";
            } else if (req.productUrl().contains("shopee.vn")) {
                name = "Sản phẩm Shopee";
            } else {
                name = "Sản phẩm";
            }
        }

        // 4. Tự động convert sang affiliate link
        // Ưu tiên affiliateUrl user truyền vào, nếu không có thì tự convert
        String linkToShare = req.productUrl(); // fallback mặc định
        if (req.affiliateUrl() != null && !req.affiliateUrl().isBlank()) {
            // User đã truyền affiliate link sẵn
            linkToShare = req.affiliateUrl();
        } else {
            // Tự động convert qua AccessTrade
            AccessTradeService.ConvertResult result = accessTradeService.convertLink(req.productUrl());
            if (result.isSuccess()) {
                linkToShare = result.affiliateUrl();
            }
            // Nếu convert thất bại → dùng productUrl gốc
        }

        // 5. Kiểm tra scrape
        boolean wasScraped = !scraped.name().isBlank();

        String priceFormatted = formatPrice(priceRaw);

        // 6. Build 3 templates
        String postZalo     = buildZalo(name, priceFormatted, linkToShare);
        String postFacebook = buildFacebook(name, priceFormatted, linkToShare);
        String postTelegram = buildTelegram(name, priceFormatted, linkToShare);

        // 7. Lưu lịch sử
        PostHistory history = new PostHistory();
        history.setUserId(userId);
        history.setProductUrl(req.productUrl());
        history.setProductName(name);
        history.setProductPrice(priceRaw);
        history.setProductImage(image);
        history.setPostZalo(postZalo);
        history.setPostFacebook(postFacebook);
        history.setPostTelegram(postTelegram);
        postHistoryRepository.save(history);

        return new GeneratePostResponse(
                name,
                priceFormatted,
                image,
                linkToShare,
                postZalo,
                postFacebook,
                postTelegram,
                wasScraped
        );
    }

    public Page<PostHistory> getHistory(UUID userId, int page) {
        return postHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, 10)
        );
    }

    // ----------------------------------------------------------------
    // Templates
    // ----------------------------------------------------------------

    private String buildZalo(String name, String price, String link) {
        return "🔥 DEAL HOT HÔM NAY! 🔥\n\n"
                + "📦 " + name + "\n\n"
                + "💰 Chỉ còn: " + price + "\n"
                + "⚡ Số lượng có hạn — mua ngay kẻo hết!\n\n"
                + "👉 Link mua: " + link + "\n\n"
                + "✅ Giao hàng nhanh | Đổi trả dễ dàng\n"
                + "📲 Chia sẻ cho bạn bè cùng mua nhé!";
    }

    private String buildFacebook(String name, String price, String link) {
        return "💥 " + name + "\n\n"
                + "Giá siêu tốt hôm nay: " + price + " 🏷️\n\n"
                + "Mình vừa tìm được deal này, anh em tranh thủ mua nhanh nhé!\n"
                + "Không biết còn đến khi nào 😅\n\n"
                + "🛒 Mua tại đây: " + link + "\n\n"
                + "#deal #muasắm #giá rẻ #affiliatemarketing";
    }

    private String buildTelegram(String name, String price, String link) {
        return "🛍 " + name + "\n\n"
                + "💵 Giá: " + price + "\n\n"
                + "Mua ngay 👇\n"
                + link + "\n\n"
                + "— Deal tốt chia sẻ từ AffKit";
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String formatPrice(long price) {
        if (price <= 0) return "Liên hệ";
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(price) + "đ";
    }

    private long parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0L;
        try { return Long.parseLong(digits); }
        catch (NumberFormatException e) { return 0L; }
    }
}