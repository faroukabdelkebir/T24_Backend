package com.t24extractor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1")
@Slf4j
public class JobLauncherController {

    JobLauncher jobLauncher;
    JobOperator jobOperator;
    Job loaderJob;
    Job mainJob;
    Job cusJob;
    Job ftJob;
    Job billDetailsJob;

    @Autowired
    public JobLauncherController(JobLauncher jobLauncher,JobOperator jobOperator,
                                 @Qualifier("loadSchema") Job loaderJob,
                                 @Qualifier("t24") Job mainJob,
                                 @Qualifier("customer") Job cusJob,
                                 @Qualifier("ft.his") Job ftJob,
                                 @Qualifier("bill.details") Job billDetailsJob
                                 ) {
        this.jobLauncher = jobLauncher;
        this.jobOperator = jobOperator;
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
        jobLauncher.run(mainJob, jobParameters);
    }

    @RequestMapping("/customer/start")
    @CrossOrigin
    public void handleCustomer() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(cusJob, jobParameters);
    }

    @RequestMapping("/ft-his/start")
    @CrossOrigin
    public void handleFt() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(ftJob, jobParameters);
    }

    @RequestMapping("/billdetails/start")
    @CrossOrigin
    public void handleBillDetails() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(billDetailsJob, jobParameters);
    }

    @RequestMapping("/stop")
    @CrossOrigin
    public void handleStoppingAllJobs() throws Exception {
        Set<Long> executions = jobOperator.getRunningExecutions("T24");
        log.info("Executions: {}", executions);
        jobOperator.stop(executions.iterator().next());
    }



}
