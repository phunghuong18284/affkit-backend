package vn.affkit.accesstrade;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "accesstrade")
public class AccessTradeConfig {

    private String apiKey;
    private String apiUrl;
    private Map<String, String> campaigns;

    public String getCampaignId(String platform) {
        if (campaigns == null) return null;
        return campaigns.get(platform.toLowerCase());
    }
}