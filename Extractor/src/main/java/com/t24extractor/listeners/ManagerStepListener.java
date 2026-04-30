//package com.t24extractor.listeners;
//
//import com.mongodb.client.model.Indexes;
//import nu.xom.Element;
//import org.bson.Document;
//import org.springframework.batch.core.ExitStatus;
//import org.springframework.batch.core.StepExecution;
//import org.springframework.batch.core.StepExecutionListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.mongodb.core.MongoOperations;
//import org.springframework.jdbc.core.JdbcOperations;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class ManagerStepListener implements StepExecutionListener {
//    @Value("${queue.BI}")
//    private String queueBI;
//
//    private String t24SelectionName;
//    private String chargement;
//
//    private MongoOperations mongoExecutor;
//    private JdbcOperations jdbcExecutor;
//    private Map<String, Object> jobParams;
//
//    public ManagerStepListener() {
//    }
//
//    public void setMongoExecutor(MongoOperations mongoOperations) {
//        this.mongoExecutor = mongoOperations;
//    }
//
//    public void setT24SelectionName(String t24Name) {
//        this.t24SelectionName = t24Name;
//    }
//
//    public void setChargement(String chargement) {
//        this.chargement = chargement;
//    }
//
//    public void setJdbcExecutor(JdbcOperations jdbcOperations) {
//        this.jdbcExecutor = jdbcOperations;
//    }
//
//    public void setJobParams(Map<String, Object> params) {
//        this.jobParams = params;
//    }
//
//    @Override
//    public void beforeStep(StepExecution stepExecution) {
//        String collection = t24SelectionName.replaceAll("\\.", "_").replaceAll("\\$", "#");
//
//        if (!mongoExecutor.collectionExists(collection)) {
//            mongoExecutor.createCollection(collection);
//            mongoExecutor.getCollection(collection).createIndex(Indexes.ascending("rowNumber"));
//
//        } else {
//            AtomicBoolean rowNumindexExist = new AtomicBoolean(false);
//            AtomicBoolean processedBiIndxExist = new AtomicBoolean(false);
//            mongoExecutor.getCollection(collection).listIndexes().forEach(doc ->
//            {
//                Document index = (Document) doc.get("key");
//
//                if (index.containsKey("rowNumber")) {
//                    rowNumindexExist.set(true);
//                }
//
//
//                if (index.containsKey("PROCESSED_BI")) {
//                    processedBiIndxExist.set(true);
//                }
//            });
//            if (!rowNumindexExist.get()){
//                mongoExecutor.getCollection(collection).createIndex(Indexes.ascending("rowNumber"));
//            }
//            if (!processedBiIndxExist.get()){
//                mongoExecutor.getCollection(collection).createIndex(Indexes.ascending("PROCESSED_BI"));
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public ExitStatus afterStep(StepExecution stepExecution) {
//        String collection = t24SelectionName.replaceAll("\\.", "_").replaceAll("\\$", "#");
//
//        if (stepExecution.getExitStatus().getExitCode().equals(ExitStatus.COMPLETED.getExitCode()) &&
//                stepExecution.getReadCount() != 0) {
//            if ("Incremental".equals(chargement)) {
//                Document result = mongoExecutor.executeCommand("{find: '" + collection + "', filter: {},projection: {_id: 0, " + jobParams.get("Colonne_Ref").toString() + ": 1}, sort: {'" + jobParams.get("Colonne_Ref").toString() + "': -1}, limit:1, singleBatch: true}");
//                Document cursor = (Document) result.get("cursor");
//
//                List<Document> docs = (List<Document>) cursor.get("firstBatch");
//
//                jdbcExecutor.execute("UPDATE batch_config SET \"Last_Date\"=" + docs.get(0).get(jobParams.get("Colonne_Ref").toString()) + " WHERE \"TableName\" = '" + jobParams.get("Table_Name").toString() + "'");
//            } else if ("Incremental-specific".equals(chargement)) {
//                jdbcExecutor.execute("UPDATE batch_config SET \"Last_Date\"=" + LocalDate.now() + " WHERE \"id\" = '" + jobParams.get("id").toString() + "'");
//            }
//
////            if (jobParams.get("Destination").equals("BI")) {
////                Element root = new Element("Table");
////                Element name = new Element("TableName");
////
////                name.appendChild(collection);
////                root.appendChild(name);
////
////                sender.send(queueBI, root.toXML());
////            }
//
//            return ExitStatus.COMPLETED;
//        } else if (stepExecution.getReadCount() == 0) {
//            return ExitStatus.COMPLETED;
//        } else {
//            return ExitStatus.FAILED;
//        }
//    }
//}
