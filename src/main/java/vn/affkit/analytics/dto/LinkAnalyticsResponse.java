package vn.affkit.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LinkAnalyticsResponse {

    private Summary summary;
    private List<ClickByDay> clicksByDay;
    private List<DeviceBreakdown> deviceBreakdown;
    private List<SourceBreakdown> sourceBreakdown;

    @Data
    @Builder
    public static class Summary {
        private long totalClicks;
        private String shortCode;
        private String originalUrl;
        private String title;
    }

    @Data
    @Builder
    public static class ClickByDay {
        private String date;
        private long clicks;
    }

    @Data
    @Builder
    public static class DeviceBreakdown {
        private String device;
        private long clicks;
        private double percentage;
    }

    @Data
    @Builder
    public static class SourceBreakdown {
        private String source;
        private long clicks;
        private double percentage;
    }
}