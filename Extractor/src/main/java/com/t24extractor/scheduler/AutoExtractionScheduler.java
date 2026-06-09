package com.t24extractor.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.t24extractor.commons.Constants.AA_BILL_DETAILS;
import static com.t24extractor.commons.Constants.CUSTOMER;
import static com.t24extractor.commons.Constants.FUNDS_TRANSFER_HIS;
import static com.t24extractor.commons.Constants.RUN_ID;

@Component
@RefreshScope
@Slf4j
public class AutoExtractionScheduler {

    private static final String SELECT_SCHEDULED_BATCHES = """
            SELECT id, scheduler_time
            FROM BATCH_CONFIG
            WHERE enabled = 1
              AND scheduler = 1
              AND scheduler_time IS NOT NULL
              AND LTRIM(RTRIM(scheduler_time)) <> ''
            """;

    private final JdbcTemplate jdbcBI;
    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job customerJob;
    private final Job ftJob;
    private final Job billDetailsJob;
    private final Job mainJob;

    private final Map<String, LocalDate> lastRunByBatch = new ConcurrentHashMap<>();

    public AutoExtractionScheduler(
            @Qualifier("jdbcBI") JdbcTemplate jdbcBI,
            JobOperator jobOperator,
            JobRepository jobRepository,
            @Qualifier("customer") Job customerJob,
            @Qualifier("ftHis") Job ftJob,
            @Qualifier("billDetails") Job billDetailsJob,
            @Qualifier("t24Job") Job mainJob
    ) {
        this.jdbcBI = jdbcBI;
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.customerJob = customerJob;
        this.ftJob = ftJob;
        this.billDetailsJob = billDetailsJob;
        this.mainJob = mainJob;
    }

    @Scheduled(fixedDelayString = "${extractor.scheduler.poll-ms:60000}")
    public void runDynamicScheduler() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> configs = jdbcBI.queryForList(SELECT_SCHEDULED_BATCHES);
        for (Map<String, Object> row : configs) {
            String batchId = String.valueOf(row.get("id"));
            String schedulerTime = String.valueOf(row.get("scheduler_time"));
            LocalTime scheduledTime = parseSchedulerTime(schedulerTime, batchId);
            if (scheduledTime == null) {
                continue;
            }

            if (!now.isBefore(scheduledTime)
                    && !today.equals(lastRunByBatch.get(batchId))) {
                launchJobForBatch(batchId, today);
            }
        }
    }

    private LocalTime parseSchedulerTime(String schedulerTime, String batchId) {
        try {
            return LocalTime.parse(schedulerTime).withSecond(0).withNano(0);
        } catch (DateTimeParseException ex) {
            log.warn("scheduler_time invalide pour {}: {}", batchId, schedulerTime);
            return null;
        }
    }

    private void launchJobForBatch(String batchId, LocalDate today) {
        Job targetJob = resolveJob(batchId);
        String jobName = targetJob.getName();
        if (!jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
            log.info("Job {} déjà en cours, skip scheduler pour {}", jobName, batchId);
            return;
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString(RUN_ID, UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobOperator.start(targetJob, jobParameters);
            lastRunByBatch.put(batchId, today);
            log.info("Extraction planifiée lancée pour {} à {}", batchId, LocalTime.now());
        } catch (JobInstanceAlreadyCompleteException
                 | JobExecutionAlreadyRunningException
                 | InvalidJobParametersException
                 | JobRestartException e) {
            throw new IllegalStateException("Unable to start scheduled job for batch " + batchId, e);
        }
    }

    private Job resolveJob(String batchId) {
        return switch (batchId) {
            case CUSTOMER -> customerJob;
            case FUNDS_TRANSFER_HIS -> ftJob;
            case AA_BILL_DETAILS -> billDetailsJob;
            default -> mainJob;
        };
    }
}
