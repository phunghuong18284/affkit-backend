package vn.affkit.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfig {
    private String botToken;
    private String botUsername;
}