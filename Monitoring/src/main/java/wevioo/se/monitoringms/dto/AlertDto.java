package wevioo.se.monitoringms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertDto {
    private String        type;
    private String        severity;
    private String        jobName;
    private String        message;
    private LocalDateTime triggeredAt;
    private boolean       resolved;
}
