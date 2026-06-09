package com.t24extractor.scheduler;

import static com.t24extractor.commons.Constants.CUSTOMER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.jdbc.core.JdbcTemplate;

class AutoExtractionSchedulerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final JobOperator operator = mock(JobOperator.class);
    private final JobRepository repository = mock(JobRepository.class);
    private final Job customer = job("customer");
    private final Job ft = job("ft");
    private final Job bill = job("bill");
    private final Job main = job("main");
    private final AutoExtractionScheduler scheduler =
            new AutoExtractionScheduler(jdbcTemplate, operator, repository, customer, ft, bill, main);

    @Test
    void launchesDueBatchOnlyOncePerDay() throws Exception {
        when(jdbcTemplate.queryForList(any(String.class)))
                .thenReturn(List.of(Map.of("id", CUSTOMER, "scheduler_time", "00:00")));
        when(repository.findRunningJobExecutions("customer")).thenReturn(Set.of());

        scheduler.runDynamicScheduler();
        scheduler.runDynamicScheduler();

        verify(operator, times(1)).start(any(Job.class), any(JobParameters.class));
        verify(operator).start(org.mockito.ArgumentMatchers.eq(customer), any(JobParameters.class));
    }

    @Test
    void skipsInvalidTimeAndAlreadyRunningJob() throws Exception {
        when(jdbcTemplate.queryForList(any(String.class)))
                .thenReturn(List.of(
                        Map.of("id", "invalid", "scheduler_time", "not-a-time"),
                        Map.of("id", CUSTOMER, "scheduler_time", "00:00")));
        when(repository.findRunningJobExecutions("customer"))
                .thenReturn(Set.of(mock(org.springframework.batch.core.job.JobExecution.class)));

        scheduler.runDynamicScheduler();

        verify(operator, never()).start(any(Job.class), any(JobParameters.class));
    }

    private Job job(String name) {
        Job job = mock(Job.class);
        when(job.getName()).thenReturn(name);
        return job;
    }
}
