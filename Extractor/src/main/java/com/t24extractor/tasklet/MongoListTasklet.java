//package com.t24extractor.tasklet;
//
//import com.mongodb.client.MongoCursor;
//import org.bson.Document;
//import org.springframework.batch.core.StepContribution;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.item.ExecutionContext;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.data.mongodb.core.MongoOperations;
//import org.springframework.util.Assert;
//
//import java.util.HashSet;
//
//public class MongoListTasklet implements Tasklet
//{
//
//    private MongoOperations mongoOperations;
//
//    private String t24_selection_name;
//
//    public void setMongoOperations(MongoOperations mongoOperations)
//    {
//        this.mongoOperations = mongoOperations;
//    }
//
//    public void setT24_selection_name(String t24_selection_name)
//    {
//        this.t24_selection_name = t24_selection_name;
//    }
//
//    @Override
//    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception
//    {
//        ExecutionContext List_Context = chunkContext.getStepContext()
//                .getStepExecution()
//                .getJobExecution()
//                .getExecutionContext() ;
//
//        MongoCursor<Document> cursor = mongoOperations.getCollection(t24_selection_name.replaceAll("\\.", "_").replace("$", "#")).find().batchSize(1000).iterator();
//
//        HashSet<String> List_Ids = new HashSet<>();
//
//        while (cursor.hasNext())
//        {
//            List_Ids.add(cursor.next().get("_id").toString());
//        }
//
//        List_Context.put("IDS", List_Ids);
//        return RepeatStatus.FINISHED;
//    }
//}
