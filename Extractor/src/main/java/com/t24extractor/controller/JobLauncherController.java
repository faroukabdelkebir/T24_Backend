package com.t24extractor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
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
                                 @Qualifier("t24") Job mainJob,
                                 @Qualifier("customer") Job cusJob,
                                 @Qualifier("ft.his") Job ftJob,
                                 @Qualifier("bill.details") Job billDetailsJob
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
    @CrossOrigin
    public void handleMain() throws Exception {

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.start(mainJob, jobParameters);
    }

    @RequestMapping("/customer/start")
    @CrossOrigin
    public void handleCustomer() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.start(cusJob, jobParameters);
    }

    @RequestMapping("/ft-his/start")
    @CrossOrigin
    public void handleFt() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.start(ftJob, jobParameters);
    }

    @RequestMapping("/billdetails/start")
    @CrossOrigin
    public void handleBillDetails() throws Exception {
        String jobName = billDetailsJob.getName();
        if (!jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
            log.warn("{} est déja en cours", jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobOperator.start(billDetailsJob, jobParameters);
    }

    @RequestMapping("/stop")
    @CrossOrigin
    public void handleStoppingAllJobs() throws Exception {
        Set<JobExecution> executions = jobRepository.findRunningJobExecutions("T24");
        log.info("Executions: {}", executions);
        executions.stream()
                .max(Comparator.comparing(JobExecution::getCreateTime))
                .ifPresent(jobExecution -> {
                    try {
                        jobOperator.stop(jobExecution);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to stop running execution " + jobExecution.getId(), e);
                    }
                });
    }



}
