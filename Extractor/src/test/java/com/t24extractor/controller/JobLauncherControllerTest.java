package com.t24extractor.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

class JobLauncherControllerTest {

    private final JobOperator operator = mock(JobOperator.class);
    private final JobRepository repository = mock(JobRepository.class);
    private final Job loader = job("loader");
    private final Job main = job("T24");
    private final Job customer = job("customer");
    private final Job ft = job("ft");
    private final Job bill = job("bill");
    private final JobLauncherController controller =
            new JobLauncherController(operator, repository, loader, main, customer, ft, bill);

    @Test
    void startsEachExposedJob() throws Exception {
        controller.handleMain();
        controller.handleCustomer();
        controller.handleFt();
        when(repository.findRunningJobExecutions("bill")).thenReturn(Set.of());
        controller.handleBillDetails();

        verify(operator).start(org.mockito.ArgumentMatchers.eq(main),
                org.mockito.ArgumentMatchers.any(JobParameters.class));
        verify(operator).start(org.mockito.ArgumentMatchers.eq(customer),
                org.mockito.ArgumentMatchers.any(JobParameters.class));
        verify(operator).start(org.mockito.ArgumentMatchers.eq(ft),
                org.mockito.ArgumentMatchers.any(JobParameters.class));
        verify(operator).start(org.mockito.ArgumentMatchers.eq(bill),
                org.mockito.ArgumentMatchers.any(JobParameters.class));
    }

    @Test
    void wrapsStartFailures() throws Exception {
        when(operator.start(any(Job.class), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("running"));

        assertThrows(IllegalStateException.class, controller::handleMain);
    }

    @Test
    void stopsMostRecentMainExecution() throws Exception {
        JobExecution oldExecution = execution(1L, LocalDateTime.now().minusMinutes(2));
        JobExecution recentExecution = execution(2L, LocalDateTime.now());
        Set<JobExecution> executions = new LinkedHashSet<>(Set.of(oldExecution, recentExecution));
        when(repository.findRunningJobExecutions("T24")).thenReturn(executions);

        controller.handleStoppingAllJobs();

        verify(operator).stop(recentExecution);
        verify(operator, never()).stop(oldExecution);
    }

    private Job job(String name) {
        Job job = mock(Job.class);
        when(job.getName()).thenReturn(name);
        return job;
    }

    private JobExecution execution(long id, LocalDateTime created) {
        JobExecution execution =
                new JobExecution(id, new JobInstance(id, "T24"), new JobParameters());
        execution.setCreateTime(created);
        return execution;
    }
}
