package com.t24extractor.listeners;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobListener implements JobExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(JobListener.class);

  @Value("${spring.application.name}")
  private String appName;

  public JobListener(@Qualifier("jdbcBI") JdbcTemplate jdbcExecutor) {
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    long jobID = jobExecution.getId();

    LOG.trace("[Job-ID : {}] Adding this Job to Extractors table", jobID);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
  //

  }
}
