//package com.t24extractor.processor;
//
//import com.t24extractor.tasklet.StandardSelectionTasklet;
////import org.bson.Document;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.beans.factory.annotation.Value;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//public class BlobProcessor implements ItemProcessor<HashMap<String, Object>, Document> {
//
//    @Value("#{jobParameters['Table_Name']}")
//    String oracleTableName;
//
//    @Override
//    public Document process(HashMap<String, Object> stringObjectHashMap) throws Exception {
//        Document document = new Document();
//        stringObjectHashMap.forEach((k, v) ->
//        {
//            if (v != null) {
//                if (k.equals("_id")){
//                    if ((oracleTableName.contains("#HIS") || oracleTableName.contains("#ARC")) && stringObjectHashMap.get(k).toString().contains(";")) {
//                        document.append("ORIGINAL_ID", v.toString().substring(0, v.toString().indexOf(";")));
//                    }else {
//                        document.append("ORIGINAL_ID", v.toString());
//                    }
//                }
//                if (StandardSelectionTasklet.MultiValueBlobFields.contains(k)) {
//                    Document multiField = new Document();
//                    multiField.append(k, v);
//                    multiField.append("mvsequence", 1);
//                    multiField.append("sequence", 1);
//                    List<Document> list = new ArrayList<>();
//                    list.add(multiField);
//                    document.append(k, list);
//                } else {
//                    document.append(k, v);
//                }
//            }
//        });
//
//        return document;
//    }
//}
