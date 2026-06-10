package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mappe BATCH_JOB_EXECUTION + BATCH_JOB_INSTANCE (jointure).
 * Utilisé en lecture seule — pas d'annotation JPA, mapping manuel via RowMapper.
 */
@Data
@Builder
public class BatchJobExecution {

    private Long    jobExecutionId;
    private Long    jobInstanceId;
    private String  jobName;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String  status;
    private String  exitCode;
    private String  exitMessage;
    private LocalDateTime lastUpdated;

    /** Durée calculée en secondes. Null si le job tourne encore. */
    public Long getDurationSeconds() {
        if (startTime == null) return null;
        var end = (endTime != null) ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }

    public boolean isRunning() {
        return "STARTED".equals(status) || "STARTING".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}