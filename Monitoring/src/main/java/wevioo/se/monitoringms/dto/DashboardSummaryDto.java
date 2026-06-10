package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vue d'ensemble renvoyée au dashboard principal.
 */
@Data
@Builder
public class DashboardSummaryDto {

    private int runningJobsCount;

    private int completedTodayCount;

    private int failedTodayCount;

    private double successRate7d;

    private String cobStatus;

    /** Date métier active (dernière date d'extraction réussie). */
    private String lastBusinessDate;

    private List<JobExecutionDto> liveJobs;

    private List<JobExecutionDto> recentExecutions;

    private List<AlertDto> activeAlerts;

    private LocalDateTime generatedAt;
}
