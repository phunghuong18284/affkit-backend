package vn.affkit.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.affkit.accesstrade.AccessTradeService;
import vn.affkit.post.dto.GeneratePostRequest;
import vn.affkit.post.dto.GeneratePostResponse;
import vn.affkit.post.entity.PostHistory;
import vn.affkit.post.repository.PostHistoryRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Tests")
class PostServiceTest {

    @Mock ScraperService scraperService;
    @Mock PostHistoryRepository postHistoryRepository;
    @Mock AccessTradeService accessTradeService;

    @InjectMocks PostService postService;

    private final UUID userId = UUID.randomUUID();

    // Helper tạo ScrapedProduct mock (4 field: name, price, imageUrl, sourceUrl)
    private vn.affkit.post.dto.ScrapedProduct scraped(String name, long price, String image) {
        return new vn.affkit.post.dto.ScrapedProduct(name, price, image, null);
    }

    private AccessTradeService.ConvertResult successResult(String affiliateUrl) {
        return AccessTradeService.ConvertResult.success("https://original.url", affiliateUrl, "tiki");
    }

    private AccessTradeService.ConvertResult failResult() {
        return AccessTradeService.ConvertResult.unsupported("https://original.url");
    }

    @Nested
    @DisplayName("generate() - affiliate link logic")
    class AffiliateLinkTests {

        @Test
        @DisplayName("✅ User truyền affiliateUrl → dùng luôn, không gọi AccessTrade")
        void generate_withAffiliateUrl_skipConvert() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "Tai nghe Sony",
                    "500000",
                    "https://shorten.asia/existing123"  // user đã có affiliate link
            );
            when(scraperService.scrape("https://tiki.vn/product/123"))
                    .thenReturn(scraped("Tai nghe Sony WH-1000XM5", 500000L, "https://img.tiki.vn/img.jpg"));
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.linkToShare()).isEqualTo("https://shorten.asia/existing123");
            verify(accessTradeService, never()).convertLink(anyString());
        }

        @Test
        @DisplayName("✅ Không có affiliateUrl → gọi AccessTrade convert thành công")
        void generate_noAffiliateUrl_convertSuccess() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/456",
                    null, null, null  // không truyền affiliateUrl
            );
            when(scraperService.scrape("https://tiki.vn/product/456"))
                    .thenReturn(scraped("Giày Nike", 1200000L, "https://img.tiki.vn/shoe.jpg"));
            when(accessTradeService.convertLink("https://tiki.vn/product/456"))
                    .thenReturn(successResult("https://shorten.asia/converted456"));
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.linkToShare()).isEqualTo("https://shorten.asia/converted456");
            verify(accessTradeService).convertLink("https://tiki.vn/product/456");
        }

        @Test
        @DisplayName("✅ AccessTrade convert thất bại → fallback về productUrl gốc, không throw")
        void generate_convertFail_fallbackToOriginal() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://shopee.vn/product/789",
                    null, null, null
            );
            when(scraperService.scrape("https://shopee.vn/product/789"))
                    .thenReturn(scraped("San pham Shopee", 0L, null));
            when(accessTradeService.convertLink("https://shopee.vn/product/789"))
                    .thenReturn(failResult()); // thất bại
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act - không được throw exception
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert - fallback về URL gốc
            assertThat(response.linkToShare()).isEqualTo("https://shopee.vn/product/789");
        }
    }

    @Nested
    @DisplayName("generate() - product name logic")
    class ProductNameTests {

        @Test
        @DisplayName("✅ User nhập tên → dùng tên của user, bỏ qua tên scrape")
        void generate_userInputName_overrideScrape() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "Ten do user nhap",
                    null, null
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("Ten scrape duoc", 500000L, null));
            when(accessTradeService.convertLink(any())).thenReturn(failResult());
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.productName()).isEqualTo("Ten do user nhap");
        }

        @Test
        @DisplayName("✅ Scrape tên thành công → dùng tên scrape")
        void generate_scrapedName_used() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    null, null, null
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("Tai nghe Sony WH-1000XM5", 3500000L, null));
            when(accessTradeService.convertLink(any())).thenReturn(failResult());
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.productName()).isEqualTo("Tai nghe Sony WH-1000XM5");
        }

        @Test
        @DisplayName("✅ Scrape thất bại + link TikTok → tên mặc định 'Sản phẩm TikTok Shop'")
        void generate_tiktokUrl_defaultName() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiktok.com/shop/product/999",
                    null, null, null
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("", 0L, null)); // scrape thất bại
            when(accessTradeService.convertLink(any())).thenReturn(failResult());
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.productName()).isEqualTo("Sản phẩm TikTok Shop");
        }

        @Test
        @DisplayName("✅ Scrape thất bại + link Shopee → tên mặc định 'Sản phẩm Shopee'")
        void generate_shopeeUrl_defaultName() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://shopee.vn/product/111",
                    null, null, null
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("", 0L, null));
            when(accessTradeService.convertLink(any())).thenReturn(failResult());
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.productName()).isEqualTo("Sản phẩm Shopee");
        }
    }

    @Nested
    @DisplayName("generate() - template content")
    class TemplateTests {

        @Test
        @DisplayName("✅ Zalo template chứa tên sản phẩm + link + giá")
        void generate_zaloTemplate_containsCorrectContent() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "Tai nghe Bluetooth",
                    "299000",
                    "https://shorten.asia/test123"
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("Tai nghe Bluetooth", 299000L, null));
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.postZalo())
                    .contains("Tai nghe Bluetooth")
                    .contains("https://shorten.asia/test123")
                    .contains("299");
        }

        @Test
        @DisplayName("✅ Giá <= 0 → hiển thị 'Liên hệ' thay vì số")
        void generate_priceZero_showLienHe() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "San pham test", "0", null
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("San pham test", 0L, null));
            when(accessTradeService.convertLink(any())).thenReturn(failResult());
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.postZalo()).contains("Liên hệ");
        }

        @Test
        @DisplayName("✅ Cả 3 template đều được tạo (không null, không rỗng)")
        void generate_allThreeTemplates_notEmpty() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "San pham A", "500000",
                    "https://shorten.asia/abc"
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("San pham A", 500000L, null));
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            GeneratePostResponse response = postService.generate(req, userId);

            // Assert
            assertThat(response.postZalo()).isNotBlank();
            assertThat(response.postFacebook()).isNotBlank();
            assertThat(response.postTelegram()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("generate() - lưu lịch sử")
    class HistoryTests {

        @Test
        @DisplayName("✅ Mỗi lần generate → lưu 1 record vào post_history")
        void generate_savesHistory() {
            // Arrange
            GeneratePostRequest req = new GeneratePostRequest(
                    "https://tiki.vn/product/123",
                    "San pham X", "100000",
                    "https://shorten.asia/xyz"
            );
            when(scraperService.scrape(any()))
                    .thenReturn(scraped("San pham X", 100000L, "https://img.jpg"));
            when(postHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            postService.generate(req, userId);

            // Assert - capture argument để kiểm tra nội dung
            ArgumentCaptor<PostHistory> captor = ArgumentCaptor.forClass(PostHistory.class);
            verify(postHistoryRepository).save(captor.capture());

            PostHistory saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getProductUrl()).isEqualTo("https://tiki.vn/product/123");
            assertThat(saved.getProductName()).isEqualTo("San pham X");
            assertThat(saved.getPostZalo()).isNotBlank();
            assertThat(saved.getPostFacebook()).isNotBlank();
            assertThat(saved.getPostTelegram()).isNotBlank();
        }
    }
}
