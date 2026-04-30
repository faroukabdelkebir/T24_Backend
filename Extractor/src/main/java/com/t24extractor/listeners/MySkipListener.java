//package com.t24extractor.listeners;
//
//import com.t24extractor.model.T24Table;
//import org.bson.Document;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.batch.core.SkipListener;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.stereotype.Component;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@StepScope
//@Component
//public class MySkipListener implements SkipListener<T24Table, Document>
//{   private static final Logger LOG = LoggerFactory.getLogger(MySkipListener.class) ;
//
//
//
//    @Autowired
//    private MongoTemplate mongoExecutor;
//
//    @Value("#{jobParameters['T24_Selection_Name']}")
//    String collection;
//
//    @Override
//    public void onSkipInRead(Throwable throwable)
//    {
//        LOG.trace("\n" + throwable.getMessage());
//    }
//
//    @Override
//    public void onSkipInWrite(Document document, Throwable throwable)
//    {
//        Document doc = new Document();
//
//        doc.append("_id", document.get("_id") + "_" + LocalDate.now());
//        doc.append("Record_id", document.get("_id"));
//        doc.append("Table", collection.replaceAll("\\.", "_").replace("$", "#"));
//        doc.append("Phase", "Writing");
//        doc.append("Exception", throwable.getMessage());
//        doc.append("Date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
//
//        mongoExecutor.save(doc, "INVALID_ROWS_"+collection.replaceAll("\\.", "_").replace("$", "#"));
//    }
//
//    @Override
//    public void onSkipInProcess(T24Table t24_table, Throwable throwable)
//    {
//        Document document = new Document();
//
//        document.append("_id", t24_table.getRecid() + "_" + LocalDate.now());
//        document.append("Record_id", t24_table.getRecid());
//        document.append("Table", collection.replaceAll("\\.", "_").replace("$", "#"));
//        document.append("Phase", "Processing");
//        document.append("Exception", throwable.getMessage());
//        document.append("Date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
//
//        mongoExecutor.save(document, "INVALID_ROWS_"+collection.replaceAll("\\.", "_").replace("$", "#"));
//    }
//}
