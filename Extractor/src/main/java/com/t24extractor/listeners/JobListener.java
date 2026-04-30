package com.t24extractor.listeners;

import com.t24extractor.model.Job;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobListener implements JobExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(JobListener.class);
  private static String BATCH_CONFIG_TABLE_NAME = "batch_config";
  private static String EXTRACTION_COLUMN_NAME = "Extraction";

  @Autowired
  @Qualifier("JdbcBI")
  private JdbcTemplate jdbcExecutor;

  @Value("${spring.application.name}")
  private String appName;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    long jobID = jobExecution.getId();

//    String sqlQuery =
//        "UPDATE extractors SET job_execution_id = " + jobID + " " + "WHERE app_name = '" + appName
//            + "'";

//    jdbcExecutor.execute(sqlQuery);

    LOG.trace("[Job-ID : " + jobID + "] Adding this Job to Extractors table");
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

//    String queryToExract =
//        "SELECT count(*) FROM " + BATCH_CONFIG_TABLE_NAME + " WHERE \"" + EXTRACTION_COLUMN_NAME
//            + "\" = 'Enabled'";
//    long nbTablesToExtract = jdbcExecutor.queryForObject(queryToExract, Long.class);
//
//    String queryExtracted = "select j.job_execution_id, "
//        + "    age(j.end_time, j.start_time) AS duration,  "
//        + "    j.status,  "
//        + "    param.string_val,  "
//        + "    sum(step.read_skip_count + step.read_count) AS read, "
//        + "    sum(step.write_count + step.write_skip_count) AS write "
//        + "from batch_job_execution j "
//        + "join batch_job_instance ji on j.job_instance_id=ji.job_instance_id "
//        + "join batch_job_execution_params param on param.job_execution_id=j.job_execution_id "
//        + "join batch_step_execution step on step.job_execution_id=j.job_execution_id "
//        + "where j.status <> 'STARTED' "
//        + "and j.status <> 'STARTING' "
//        + "and ji.job_name='T24' "
//        + "and (LOWER(step.step_name) not like LOWER('%Worker%') ) "
//        + "and param.key_name='T24_Selection_Name' "
//        + "and  j.create_time> "
//        + "(select max(b.create_time) "
//        + "from batch_job_execution b join batch_job_instance i on b.job_instance_id=i.job_instance_id "
//        + "where i.job_name='Injector' and b.create_time> (NOW() - interval '200' day) ) "
//        + "GROUP BY j.job_execution_id,j.create_time, param.string_val";
//
//    var extractedTables = jdbcExecutor.query(queryExtracted,
//        (rs, rowNum) -> new Job(rs.getLong("job_execution_id"), rs.getString("status"),
//            rs.getString("duration"),
//            rs.getString("string_val"), rs.getInt("read"), rs.getInt("write")));
//
//    //Add the current job
//    Integer readCount = Math.toIntExact(
//        jobExecution.getStepExecutions().stream().filter(step -> step.getStepName().equals(""))
//            .map(step -> step.getReadCount() + step.getReadSkipCount()).reduce(0L, Long::sum));
//    Integer writeCount = Math.toIntExact(
//        jobExecution.getStepExecutions().stream().filter(step -> step.getStepName().equals(""))
//            .map(step -> step.getWriteCount() + step.getWriteSkipCount()).reduce(0L, Long::sum));
//
//    extractedTables.add(new Job(jobExecution.getJobId(), jobExecution.getStatus().toString(),
//        String.valueOf(Duration.between(jobExecution.getStartTime(), LocalDateTime.now())),
//        jobExecution.getJobParameters().getString("T24_Selection_Name"), readCount, writeCount));
//
//    if (extractedTables != null && extractedTables.size() >= nbTablesToExtract) {
//      // send email
//      try {
////        emailService.sendFinalEmail(extractedTables);
//      } catch (MessagingException e) {
//        LOG.error("Un probleme s'est produit lors de l'envoi d'Email" + e.getMessage());
//      }
//    }
//
//    long jobID = jobExecution.getId().longValue();
//    String sqlQuery =
//        "UPDATE extractors SET job_execution_id = NULL " + "WHERE app_name = '" + appName + "'";
//    jdbcExecutor.execute(sqlQuery);
//    LOG.trace("[Job-ID : " + jobID + "] Deleting this Job from Extractors table");

//    	if (jobExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())){
//			try {
//				emailService.sendMail(jobExecution.getJobParameters().getString("T24_Selection_Name"), jobExecution.getAllFailureExceptions().toString());
//			} catch (MessagingException e) {
//				LOG.error("Un probleme s'est produit lors de l'envoi d'Email"+e.getMessage());
//			}
//		}
//    	else {

//		}
  }
}
