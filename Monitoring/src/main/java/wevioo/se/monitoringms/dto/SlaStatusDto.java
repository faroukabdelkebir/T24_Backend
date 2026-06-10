package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class SlaStatusDto {
    private String        jobName;
    private LocalTime slaDeadline;
    private LocalDateTime lastCompletionTime;
    private boolean       slaBreached;
    private long          lateBySeconds;      // 0 si dans les délais
    private String        status;
}