package vn.affkit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl = "http://localhost:3000";
    private String shortUrlBase = "http://localhost:8080/go/";

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }

    public String getShortUrlBase() { return shortUrlBase; }
    public void setShortUrlBase(String shortUrlBase) { this.shortUrlBase = shortUrlBase; }
}