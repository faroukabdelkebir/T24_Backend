package wevioo.se.monitoringms.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Mappe BATCH_STEP_EXECUTION.
 * Contient les métriques essentielles : read_count, write_count, skip, rollback.
 */
@Data
@Builder
public class BatchStepExecution {

    private Long    stepExecutionId;
    private Long    jobExecutionId;
    private String  stepName;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String  status;
    private Long    commitCount;
    private Long    readCount;
    private Long    filterCount;
    private Long    writeCount;
    private Long    readSkipCount;
    private Long    writeSkipCount;
    private Long    processSkipCount;
    private Long    rollbackCount;
    private String  exitCode;
    private String  exitMessage;
    private LocalDateTime lastUpdated;

    public long getTotalSkipCount() {
        return safeVal(readSkipCount) + safeVal(processSkipCount) + safeVal(writeSkipCount);
    }

    public Long getDurationSeconds() {
        if (startTime == null) return null;
        var end = (endTime != null) ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }

    /** Débit : items écrits par seconde. */
    public Double getThroughputPerSecond() {
        Long dur = getDurationSeconds();
        if (dur == null || dur == 0 || writeCount == null) return null;
        return (double) writeCount / dur;
    }

    private long safeVal(Long v) {
        return v == null ? 0L : v;
    }
}

