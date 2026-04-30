//package com.t24extractor.listeners;
//
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ApplicationStartupListener
//{
//	private static final Logger LOG = LoggerFactory.getLogger(ApplicationStartupListener.class) ;
//
//	@Autowired
//	@Qualifier("JdbcBI")
//	private JdbcTemplate jdbcExecutor ;
//
//	@Value("${spring.application.name}")
//	private String appName;
//
//	@PostConstruct
//	public void addApplicationName()
//	{
//		String sqlQuery = "INSERT INTO extractors (app_name) VALUES ('" + appName + "')" ;
//
//		jdbcExecutor.execute(sqlQuery);
//
//		LOG.trace("[" + appName + "] Adding this instance to Extractors table") ;
//	}
//}
