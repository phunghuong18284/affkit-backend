package vn.affkit.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.affkit.analytics.dto.AnalyticsOverviewResponse;
import vn.affkit.analytics.dto.LinkAnalyticsResponse;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.entity.LinkClick;
import vn.affkit.link.repository.LinkClickRepository;
import vn.affkit.link.repository.LinkRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final LinkClickRepository linkClickRepository;
    private final LinkRepository linkRepository;

    public AnalyticsOverviewResponse getOverview(String userIdStr, String period) {
        UUID userId = UUID.fromString(userIdStr);

        LocalDate to = LocalDate.now();
        LocalDate from = switch (period) {
            case "today" -> to;
            case "7d"    -> to.minusDays(7);
            case "90d"   -> to.minusDays(90);
            default      -> to.minusDays(30);
        };

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant   = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<LinkClick> clicks = linkClickRepository
                .findByUserIdAndClickedAtBetween(userId, fromInstant, toInstant);

        long totalLinks  = linkRepository.countByUserIdAndDeletedFalse(userId);
        long totalClicks = clicks.size();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        Map<String, Long> byDay = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> fmt.format(c.getClickedAt()),
                        Collectors.counting()
                ));

        List<AnalyticsOverviewResponse.ClickByDay> clicksByDay = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> AnalyticsOverviewResponse.ClickByDay.builder()
                        .date(e.getKey())
                        .clicks(e.getValue())
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> bySource = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getSource() != null ? c.getSource() : "OTHER",
                        Collectors.counting()
                ));

        List<AnalyticsOverviewResponse.SourceBreakdown> sourceBreakdown = bySource.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> AnalyticsOverviewResponse.SourceBreakdown.builder()
                        .source(e.getKey())
                        .clicks(e.getValue())
                        .percentage(totalClicks > 0 ? (e.getValue() * 100.0 / totalClicks) : 0)
                        .build())
                .collect(Collectors.toList());

        String topSource = bySource.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        Map<String, Long> byDevice = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getDeviceType() != null ? c.getDeviceType() : "OTHER",
                        Collectors.counting()
                ));

        List<AnalyticsOverviewResponse.DeviceBreakdown> deviceBreakdown = byDevice.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> AnalyticsOverviewResponse.DeviceBreakdown.builder()
                        .device(e.getKey())
                        .clicks(e.getValue())
                        .percentage(totalClicks > 0 ? (e.getValue() * 100.0 / totalClicks) : 0)
                        .build())
                .collect(Collectors.toList());

        return AnalyticsOverviewResponse.builder()
                .summary(AnalyticsOverviewResponse.Summary.builder()
                        .totalClicks(totalClicks)
                        .totalLinks(totalLinks)
                        .activeLinks(totalLinks)
                        .topSource(topSource)
                        .growthPercent(0)
                        .build())
                .clicksByDay(clicksByDay)
                .sourceBreakdown(sourceBreakdown)
                .deviceBreakdown(deviceBreakdown)
                .build();
    }

    public LinkAnalyticsResponse getLinkAnalytics(String linkIdStr, String userIdStr, String period) {
        UUID linkId = UUID.fromString(linkIdStr);
        UUID userId = UUID.fromString(userIdStr);

        var link = linkRepository.findByIdAndUserIdAndDeletedFalse(linkId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.LINK_NOT_FOUND));

        LocalDate to = LocalDate.now();
        LocalDate from = switch (period) {
            case "today" -> to;
            case "7d"    -> to.minusDays(7);
            case "90d"   -> to.minusDays(90);
            default      -> to.minusDays(30);
        };

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant   = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<LinkClick> clicks = linkClickRepository
                .findByLinkIdAndClickedAtBetween(linkId, fromInstant, toInstant);

        long totalClicks = clicks.size();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        List<LinkAnalyticsResponse.ClickByDay> clicksByDay = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> fmt.format(c.getClickedAt()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> LinkAnalyticsResponse.ClickByDay.builder()
                        .date(e.getKey()).clicks(e.getValue()).build())
                .collect(Collectors.toList());

        Map<String, Long> byDevice = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getDeviceType() != null ? c.getDeviceType() : "OTHER",
                        Collectors.counting()
                ));

        List<LinkAnalyticsResponse.DeviceBreakdown> deviceBreakdown = byDevice.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> LinkAnalyticsResponse.DeviceBreakdown.builder()
                        .device(e.getKey())
                        .clicks(e.getValue())
                        .percentage(totalClicks > 0 ? (e.getValue() * 100.0 / totalClicks) : 0)
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> bySource = clicks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getSource() != null ? c.getSource() : "OTHER",
                        Collectors.counting()
                ));

        List<LinkAnalyticsResponse.SourceBreakdown> sourceBreakdown = bySource.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> LinkAnalyticsResponse.SourceBreakdown.builder()
                        .source(e.getKey())
                        .clicks(e.getValue())
                        .percentage(totalClicks > 0 ? (e.getValue() * 100.0 / totalClicks) : 0)
                        .build())
                .collect(Collectors.toList());

        return LinkAnalyticsResponse.builder()
                .summary(LinkAnalyticsResponse.Summary.builder()
                        .totalClicks(totalClicks)
                        .shortCode(link.getShortCode())
                        .originalUrl(link.getOriginalUrl())
                        .title(link.getTitle())
                        .build())
                .clicksByDay(clicksByDay)
                .deviceBreakdown(deviceBreakdown)
                .sourceBreakdown(sourceBreakdown)
                .build();
    }
}