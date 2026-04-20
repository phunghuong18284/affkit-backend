package vn.affkit.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AnalyticsOverviewResponse {

    private Summary summary;
    private List<ClickByDay> clicksByDay;
    private List<SourceBreakdown> sourceBreakdown;
    private List<DeviceBreakdown> deviceBreakdown;

    @Data
    @Builder
    public static class Summary {
        private long totalClicks;
        private long totalLinks;
        private long activeLinks;
        private String topSource;
        private double growthPercent;
    }

    @Data
    @Builder
    public static class ClickByDay {
        private String date;
        private long clicks;
    }

    @Data
    @Builder
    public static class SourceBreakdown {
        private String source;
        private long clicks;
        private double percentage;
    }

    @Data
    @Builder
    public static class DeviceBreakdown {
        private String device;
        private long clicks;
        private double percentage;
    }
}