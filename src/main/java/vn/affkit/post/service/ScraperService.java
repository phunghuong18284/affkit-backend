package vn.affkit.post.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.affkit.post.dto.ScrapedProduct;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public ScrapedProduct scrape(String url) {
        try {
            if (url.contains("lazada.vn"))   return scrapeLazada(url);
            if (url.contains("tiki.vn"))     return scrapeTiki(url);
            if (url.contains("shopee.vn"))   return scrapeShopee(url);
            if (url.contains("tiktok.com"))  return scrapeTikTok(url);
        } catch (Exception e) {
            log.error("Scrape lỗi [{}]: {}", url, e.getMessage());
        }
        return new ScrapedProduct("", 0L, "", url);
    }

    // ----------------------------------------------------------------
    // LAZADA — dùng Jsoup + og meta tags
    // ----------------------------------------------------------------
    private ScrapedProduct scrapeLazada(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
                .header("Accept-Language", "vi-VN,vi;q=0.9")
                .timeout(15000)
                .get();

        String name  = metaContent(doc, "og:title");
        String image = metaContent(doc, "og:image");

        String priceText = "";
        for (String sel : new String[]{
                ".pdp-price_type_normal",
                "[class*='pdp-price']",
                "span[class*='price']"
        }) {
            Element el = doc.selectFirst(sel);
            if (el != null) { priceText = el.text(); break; }
        }

        return new ScrapedProduct(
                name  != null ? name.trim()  : "",
                parsePrice(priceText),
                image != null ? image.trim() : "",
                url
        );
    }

    // ----------------------------------------------------------------
    // TIKI — dùng Tiki public API, extract productId từ URL
    // ----------------------------------------------------------------
    private ScrapedProduct scrapeTiki(String url) throws Exception {
        // URL dạng: https://tiki.vn/ten-san-pham-p{productId}.html
        String productId = extractTikiProductId(url);
        if (productId == null) {
            log.warn("Không extract được productId từ Tiki URL: {}", url);
            return new ScrapedProduct("", 0L, "", url);
        }

        String apiUrl = "https://tiki.vn/api/v2/products/" + productId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "application/json")
                .header("Referer", "https://tiki.vn/")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Tiki API trả về status {}", response.statusCode());
            return new ScrapedProduct("", 0L, "", url);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String name  = root.path("name").asText("");
        long   price = root.path("price").asLong(0L);
        String image = root.path("thumbnail_url").asText("");

        return new ScrapedProduct(name.trim(), price, image.trim(), url);
    }

    private String extractTikiProductId(String url) {
        // Tiki URL: /ten-san-pham-p123456789.html
        Matcher m = Pattern.compile("-p(\\d+)\\.html").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    // ----------------------------------------------------------------
    // SHOPEE — dùng Shopee public API, extract shopId + itemId từ URL
    // ----------------------------------------------------------------
    private ScrapedProduct scrapeShopee(String url) throws Exception {
        // URL dạng: https://shopee.vn/ten-san-pham-i.{shopId}.{itemId}
        long[] ids = extractShopeeIds(url);
        if (ids == null) {
            log.warn("Không extract được shopId/itemId từ Shopee URL: {}", url);
            return new ScrapedProduct("", 0L, "", url);
        }

        long shopId = ids[0];
        long itemId = ids[1];
        String apiUrl = "https://shopee.vn/api/v4/item/get?shopid=" + shopId + "&itemid=" + itemId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "application/json")
                .header("Referer", "https://shopee.vn/")
                .header("X-Requested-With", "XMLHttpRequest")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Shopee API trả về status {}", response.statusCode());
            return new ScrapedProduct("", 0L, "", url);
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode item = root.path("data");

        String name  = item.path("name").asText("");
        // Shopee trả giá theo đơn vị xu (x100), chia 100000 để ra VND
        long   priceRaw = item.path("price").asLong(0L);
        long   price = priceRaw / 100000;
        String image = item.path("image").asText("");
        if (!image.isBlank()) {
            image = "https://down-vn.img.susercontent.com/file/" + image;
        }

        return new ScrapedProduct(name.trim(), price, image.trim(), url);
    }

    private long[] extractShopeeIds(String url) {
        // Shopee URL: /ten-san-pham-i.{shopId}.{itemId}
        Matcher m = Pattern.compile("-i\\.(\\d+)\\.(\\d+)").matcher(url);
        if (m.find()) {
            return new long[]{ Long.parseLong(m.group(1)), Long.parseLong(m.group(2)) };
        }
        return null;
    }

    // ----------------------------------------------------------------
    // TIKTOK SHOP — không scrape được, trả về rỗng để user nhập thủ công
    // ----------------------------------------------------------------
    private ScrapedProduct scrapeTikTok(String url) {
        log.info("TikTok Shop không hỗ trợ auto scrape, user cần nhập thủ công");
        return new ScrapedProduct("", 0L, "", url);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private String metaContent(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        if (el != null) return el.attr("content");
        el = doc.selectFirst("meta[name=" + property + "]");
        return el != null ? el.attr("content") : null;
    }

    private long parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0L;
        try { return Long.parseLong(digits); }
        catch (NumberFormatException e) { return 0L; }
    }
}