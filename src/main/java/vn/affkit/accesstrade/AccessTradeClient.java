package vn.affkit.accesstrade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTradeClient {

    private final AccessTradeConfig config;
    private final RestTemplate restTemplate;

    public String createAffiliateLink(String campaignId, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "token " + config.getApiKey());

            Map<String, Object> body = Map.of(
                    "campaign_id", campaignId,
                    "urls", List.of(url)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getApiUrl(), request, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof Map data) {
                    Object successObj = data.get("success_link");
                    // Tiki/Lazada tra ve List
                    if (successObj instanceof List successLinks && !successLinks.isEmpty()) {
                        Map firstLink = (Map) successLinks.get(0);
                        return (String) firstLink.get("short_link");
                    }
                    // TikTok Shop co the tra ve Map thay vi List
                    if (successObj instanceof Map firstLink) {
                        return (String) firstLink.get("short_link");
                    }
                }
            }
        } catch (Exception e) {
            log.error("AccessTrade API error for url {}: {}", url, e.getMessage());
        }
        return null;
    }
}