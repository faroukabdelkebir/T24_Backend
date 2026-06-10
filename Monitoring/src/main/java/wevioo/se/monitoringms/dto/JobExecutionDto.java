package wevioo.se.monitoringms.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class JobExecutionDto {

    private Long          jobExecutionId;
    private Long          jobInstanceId;
    private String        jobName;
    private String        status;
    private String        exitCode;
    private String        exitMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private Long          durationSeconds;
    private boolean       running;
    private boolean       failed;

    private long totalReadCount;
    private long totalWriteCount;
    private long totalSkipCount;

    /** Débit global en items/sec. */
    private Double throughputPerSecond;

    private ReconciliationDto reconciliation;
}
