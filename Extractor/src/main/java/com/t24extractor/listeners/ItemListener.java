//package com.t24extractor.listeners;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import org.bson.Document;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.core.listener.ItemListenerSupport;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.stereotype.Component;
//import com.t24extractor.model.T24Table;
//
//@Component
//@StepScope
//public class ItemListener extends ItemListenerSupport<T24Table, Document>
//{
//	@Autowired
//    private MongoTemplate mongoExecutor;
//
//	@Value("#{jobParameters['T24_Selection_Name']}")
//	private String collection;
//
//	public void onProcessError(T24Table item, Exception e)
//	{
//		Document document = new Document();
//
//		document.append("_id", item.getRecid() + "_" + LocalDate.now());
//		document.append("Record_id", item.getRecid());
//		document.append("Table", collection.replaceAll("\\.", "_").replace("$", "#"));
//		document.append("Phase", "Processing");
//		document.append("Exception", e.getMessage());
//		document.append("Date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
//
//		mongoExecutor.save(document, "INVALID_ROWS_" + collection.replaceAll("\\.", "_").replace("$", "#"));
//	}
//}
