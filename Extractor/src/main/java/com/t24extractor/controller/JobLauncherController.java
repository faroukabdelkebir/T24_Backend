package com.t24extractor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

import static com.t24extractor.commons.Constants.RUN_ID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class JobLauncherController {

    JobOperator jobOperator;
    JobRepository jobRepository;
    Job loaderJob;
    Job mainJob;
    Job cusJob;
    Job ftJob;
    Job billDetailsJob;

    @Autowired
    public JobLauncherController(JobOperator jobOperator, JobRepository jobRepository,
                                 @Qualifier("loadSchema") Job loaderJob,
                                 @Qualifier("t24Job") Job mainJob,
                                 @Qualifier("customer") Job cusJob,
                                 @Qualifier("ftHis") Job ftJob,
                                 @Qualifier("billDetails") Job billDetailsJob
                                 ) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.loaderJob = loaderJob;
        this.mainJob = mainJob;
        this.cusJob = cusJob;
        this.ftJob = ftJob;
        this.billDetailsJob = billDetailsJob;
    }

    @RequestMapping("/start")
    public void handleMain()  {

        JobParameters jobParameters = new JobParametersBuilder()
                .addString(RUN_ID, UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobOperator.start(mainJob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException
                 | JobExecutionAlreadyRunningException
                 | InvalidJobParametersException
                 | JobRestartException e) {
            throw new IllegalStateException("Unable to start main job", e);
        }
    }

    @RequestMapping("/customer/start")
    public void handleCustomer()  {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString(RUN_ID, UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobOperator.start(cusJob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException
                 | JobExecutionAlreadyRunningException
                 | InvalidJobParametersException
                 | JobRestartException e) {
            throw new IllegalStateException("Unable to start customer job", e);
        }
    }

    @RequestMapping("/ft-his/start")
    public void handleFt() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString(RUN_ID, UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobOperator.start(ftJob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException
                 | JobExecutionAlreadyRunningException
                 | InvalidJobParametersException
                 | JobRestartException e) {
            throw new IllegalStateException("Unable to start ft job", e);
        }
    }

    @RequestMapping("/billdetails/start")
    public void handleBillDetails() throws Exception {
        String jobName = billDetailsJob.getName();
        if (!jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
            log.warn("{} est déja en cours", jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString(RUN_ID, UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.start(billDetailsJob, jobParameters);
    }

    @RequestMapping("/stop")
    public void handleStoppingAllJobs()  {
        Set<JobExecution> executions = jobRepository.findRunningJobExecutions("T24");
        log.info("Executions: {}", executions);
        executions.stream()
                .max(Comparator.comparing(JobExecution::getCreateTime))
                .ifPresent(jobExecution -> {
                    try {
                        jobOperator.stop(jobExecution);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to stop running execution " + jobExecution.getId(), e);
                    }
                });
    }



}
