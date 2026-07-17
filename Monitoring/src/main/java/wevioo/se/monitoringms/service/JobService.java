package wevioo.se.monitoringms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import wevioo.se.monitoringms.dto.JobExecutionDto;
import wevioo.se.monitoringms.dto.ReconciliationDto;
import wevioo.se.monitoringms.dto.StepExecutionDto;
import wevioo.se.monitoringms.model.BatchJobExecution;
import wevioo.se.monitoringms.model.BatchStepExecution;
import wevioo.se.monitoringms.repository.BatchExecutionRepository;
import wevioo.se.monitoringms.repository.StatsRepository;

import java.util.List;

/**
 * Service pour la navigation dans l'historique des jobs et
 * le drill-down jusqu'au niveau step.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final BatchExecutionRepository batchRepo;
    private final StatsRepository statsRepo;
    private final DisplayTranslationService displayTranslationService;

    // ── Liste ────────────────────────────────────────────────────────────────

    public List<JobExecutionDto> getRecent(int limit) {
        return batchRepo.findRecent(limit).stream()
                .map(je -> enrich(je, true))
                .toList();
    }

    public List<JobExecutionDto> getByDays(int days) {
        return batchRepo.findByDays(days).stream()
                .map(je -> enrich(je, false))
                .toList();
    }

    public List<JobExecutionDto> getByJobName(String jobName, int limit) {
        return batchRepo.findByJobName(jobName, limit).stream()
                .map(je -> enrich(je, true))
                .toList();
    }

    // ── Détail ───────────────────────────────────────────────────────────────

    public JobExecutionDto getById(long jobExecutionId) {
        return batchRepo.findById(jobExecutionId)
                .map(je -> enrich(je, true))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exécution de traitement introuvable : " + jobExecutionId));
    }

    // ── Steps ────────────────────────────────────────────────────────────────

    public List<StepExecutionDto> getSteps(long jobExecutionId) {
        // Vérifie que le job existe
        batchRepo.findById(jobExecutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exécution de traitement introuvable : " + jobExecutionId));

        return batchRepo.findStepsByJobExecutionId(jobExecutionId).stream()
                .map(this::toStepDto)
                .toList();
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private JobExecutionDto enrich(BatchJobExecution je, boolean withStepAggregates) {
        long readCount  = 0;
        long writeCount = 0;
        long skipCount  = 0;
        Double throughput = null;

        if (withStepAggregates) {
            List<BatchStepExecution> steps =
                    batchRepo.findStepsByJobExecutionId(je.getJobExecutionId());

            readCount  = steps.stream().mapToLong(s -> s.getReadCount()  != null ? s.getReadCount()  : 0).sum();
            writeCount = steps.stream().mapToLong(s -> s.getWriteCount() != null ? s.getWriteCount() : 0).sum();
            skipCount  = steps.stream().mapToLong(BatchStepExecution::getTotalSkipCount).sum();

            Long dur = je.getDurationSeconds();
            if (dur != null && dur > 0 && writeCount > 0) {
                throughput = (double) writeCount / dur;
            }
        }

        // Réconciliation pour ce job
        ReconciliationDto reconciliation = resolveReconciliation(je.getJobExecutionId());

        return JobExecutionDto.builder()
                .jobExecutionId(je.getJobExecutionId())
                .jobInstanceId(je.getJobInstanceId())
                .jobName(je.getJobName())
                .status(je.getStatus())
                .exitCode(je.getExitCode())
                .exitMessage(truncate(je.getExitMessage(), 500))
                .startTime(je.getStartTime())
                .endTime(je.getEndTime())
                .createTime(je.getCreateTime())
                .durationSeconds(je.getDurationSeconds())
                .running(je.isRunning())
                .failed(je.isFailed())
                .totalReadCount(readCount)
                .totalWriteCount(writeCount)
                .totalSkipCount(skipCount)
                .throughputPerSecond(throughput)
                .reconciliation(reconciliation)
                .build();
    }

    private StepExecutionDto toStepDto(BatchStepExecution se) {
        return StepExecutionDto.builder()
                .stepExecutionId(se.getStepExecutionId())
                .jobExecutionId(se.getJobExecutionId())
                .stepName(se.getStepName())
                .status(se.getStatus())
                .exitCode(se.getExitCode())
                .startTime(se.getStartTime())
                .endTime(se.getEndTime())
                .durationSeconds(se.getDurationSeconds())
                .readCount(se.getReadCount()  != null ? se.getReadCount()  : 0)
                .writeCount(se.getWriteCount() != null ? se.getWriteCount() : 0)
                .filterCount(se.getFilterCount() != null ? se.getFilterCount() : 0)
                .skipCount(se.getTotalSkipCount())
                .rollbackCount(se.getRollbackCount() != null ? se.getRollbackCount() : 0)
                .commitCount(se.getCommitCount()   != null ? se.getCommitCount()   : 0)
                .throughputPerSecond(se.getThroughputPerSecond())
                .build();
    }

    private ReconciliationDto resolveReconciliation(long jobExecutionId) {
        return statsRepo.findByTable("", 1).stream()
                .filter(s -> s.getJobExecutionId() == jobExecutionId)
                .findFirst()
                .map(s -> ReconciliationDto.builder()
                        .tableName(s.getMssqlTable())
                        .businessDate(s.getBusinessDate())
                        .oracleCount(s.getOracleCount())
                        .sqlServerCount(s.getSqlCount())
                        .delta(s.getOracleCount() - s.getSqlCount())
                        .reconciliationRate(s.getReconciliationRate() != null ? s.getReconciliationRate() : 0.0)
                        .reconciled(s.isReconciled(5.0))
                        .status(s.isReconciled(5.0) ? "Conforme" : "Non conforme")
                        .build())
                .orElse(null);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}

