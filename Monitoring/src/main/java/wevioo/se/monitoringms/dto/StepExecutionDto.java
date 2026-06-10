package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StepExecutionDto {

    private Long          stepExecutionId;
    private Long          jobExecutionId;
    private String        stepName;
    private String        status;
    private String        exitCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long          durationSeconds;
    private long          readCount;
    private long          writeCount;
    private long          filterCount;
    private long          skipCount;
    private long          rollbackCount;
    private long          commitCount;
    private Double        throughputPerSecond;
}

