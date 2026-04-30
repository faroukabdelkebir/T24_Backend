package com.t24extractor.processor;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONPObject;
import com.t24extractor.model.T24Table;
//import com.mongodb.BasicDBList;
import lombok.Setter;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;
//import org.bson.Document;
import org.json.JSONObject;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class XMLProcessor implements ItemProcessor<T24Table, com.alibaba.fastjson2.JSONObject> {

    @Value("#{jobExecutionContext['Mongo_Standard_Selection']}")
    String ss;

    @Setter
    String oracleTableName;

    @Override
    public com.alibaba.fastjson2.JSONObject process(final T24Table t24TableItem) throws Exception {

        //Initializing SS fields from Job Execution Context
        final JSONObject ts = new JSONObject(ss);
        final JSONObject sysFieldJs = ts.getJSONArray("sysField").getJSONObject(0);
        final JSONObject sysTypeFieldJs = ts.getJSONArray("typeField").getJSONObject(0);
        final JSONObject usrFieldJs = ts.getJSONArray("usrField").getJSONObject(0);
        final JSONObject usrTypeFieldJs = ts.getJSONArray("UsrFieldType").getJSONObject(0);
        final String local = ts.getJSONArray("localRefPos").get(0).toString();

        final String recID = t24TableItem.getRecid();

//        Document document = new Document();
        com.alibaba.fastjson2.JSONObject jsonObject = new com.alibaba.fastjson2.JSONObject();


//        document.append("_id", recID);
        jsonObject.put("_id", recID);

        if ((oracleTableName.contains("#HIS") || oracleTableName.contains("#ARC")) && recID.contains(";")) {
//            document.append("ORIGINAL_ID", recID.substring(0, recID.indexOf(";")));
            jsonObject.put("ORIGINAL_ID", recID.substring(0, recID.indexOf(";")));
        }else {
//            document.append("ORIGINAL_ID", recID);
            jsonObject.put("ORIGINAL_ID", recID);
        }
        final String str_xml = t24TableItem.getXml_record();
        final Builder parser = new Builder();
        final InputStream is = new ByteArrayInputStream(str_xml.getBytes(StandardCharsets.UTF_8));
        final nu.xom.Document xmlRecord = parser.build(is);
        final Element element = xmlRecord.getRootElement();
        final Elements Children = element.getChildElements();
//        List<Document> LR_MultiValue;
        List<com.alibaba.fastjson2.JSONObject> localRefMult;
        for (int i = 0; i < Children.size(); i++) {

            if (Children.get(i).getValue().trim().isEmpty()) {
                element.removeChild(Children.get(i));
            } else if (!sysFieldJs.has(Children.get(i).getLocalName())) {
            } else if (sysTypeFieldJs.get(Children.get(i).getLocalName()).equals("M") && !Children.get(i).getLocalName().equals(local) && !sysFieldJs.get(Children.get(i).getLocalName()).toString().equals("DATE_TIME") && !Children.get(i).getValue().trim().equals("")) {
//                BasicDBList multi = new BasicDBList();
                JSONArray jsonArray = new JSONArray();
                for (Element child : element.getChildElements(Children.get(i).getLocalName())) {
                    int attrCount = child.getAttributeCount();
                    if (attrCount == 1) {
                        if (child.getAttribute("m") == null) {
                            //Document hash1 = new Document();
                            com.alibaba.fastjson2.JSONObject jsonHash = new com.alibaba.fastjson2.JSONObject();
//                            hash1.append(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
                            jsonHash.put(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
//                            hash1.append("sequence", Integer.valueOf(child.getAttributeValue("s")));
                            jsonHash.put("sequence", Integer.valueOf(child.getAttributeValue("s")));
//                            hash1.append("mvsequence", 1);
                            jsonHash.put("mvsequence", 1);
//                            multi.add(hash1);
                            jsonArray.add(jsonHash);
                        } else {
//                            Document hash = new Document();
                            com.alibaba.fastjson2.JSONObject mvSeqObject = new com.alibaba.fastjson2.JSONObject();
//                            hash.append(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
                            mvSeqObject.put(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
//                            hash.append("mvsequence", Integer.valueOf(child.getAttributeValue("m")));
                            mvSeqObject.put("mvsequence", Integer.valueOf(child.getAttributeValue("m")));
//                            hash.append("sequence", 1);
                            mvSeqObject.put("sequence", 1);
//                            multi.add(hash);
                            jsonArray.add(mvSeqObject);
                        }
                    } else if (attrCount == 2) {
//                        Document hash2 = new Document();
                        com.alibaba.fastjson2.JSONObject bothSeqObject = new com.alibaba.fastjson2.JSONObject();
//                        hash2.append(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
                        bothSeqObject.put(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
//                        hash2.append("mvsequence", Integer.valueOf(child.getAttributeValue("m")));
                        bothSeqObject.put("mvsequence", Integer.valueOf(child.getAttributeValue("m")));
//                        hash2.append("sequence", Integer.valueOf(child.getAttributeValue("s")));
                        bothSeqObject.put("sequence", Integer.valueOf(child.getAttributeValue("s")));
//                        multi.add(hash2);
                        jsonArray.add(bothSeqObject);
                    } else if (attrCount == 0) {
//                        Document hash0 = new Document();
                        com.alibaba.fastjson2.JSONObject noSeqObject = new com.alibaba.fastjson2.JSONObject();

//                        hash0.append(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());
                        noSeqObject.put(sysFieldJs.get(child.getLocalName()).toString(), child.getValue());


//                        hash0.append("mvsequence", 1);
                        noSeqObject.put("mvsequence", 1);
//                        hash0.append("sequence", 1);
                        noSeqObject.put("sequence", 1);
//                        multi.add(hash0);
                        jsonArray.add(noSeqObject);
                    }
//                    document.append(sysFieldJs.get(child.getLocalName()).toString(), multi);
                    jsonObject.put(sysFieldJs.get(child.getLocalName()).toString(), jsonArray);
                }
            } else if (sysFieldJs.get(Children.get(i).getLocalName()).toString().equals("DATE_TIME") || sysTypeFieldJs.get(Children.get(i).getLocalName()).equals("S") && !Children.get(i).getLocalName().equals(local)) {

//                document.append(sysFieldJs.get(Children.get(i).getLocalName()).toString(), Children.get(i).getValue());
                jsonObject.put(sysFieldJs.get(Children.get(i).getLocalName()).toString(), Children.get(i).getValue());


            } else if (Children.get(i).getLocalName().equals(local)) {
                if (usrFieldJs.isEmpty()) {
                    break;
                } else {
                    int attributeCnt = Children.get(i).getAttributeCount();

                    if (attributeCnt == 0) {
                        Children.get(i).addAttribute(new Attribute("m", "1"));
                    }

                    if (usrTypeFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString().equals("S")) {
//                        document.append(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), Children.get(i).getValue());
                        jsonObject.put(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), Children.get(i).getValue());

                    } else {

//                        Document hash = new Document();
                        com.alibaba.fastjson2.JSONObject hash = new com.alibaba.fastjson2.JSONObject();

                        if (Children.get(i).getAttribute("s") == null) {
//                            LR_MultiValue = new ArrayList<>();
                            localRefMult = new ArrayList<>();
                            hash.put(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), Children.get(i).getValue());
                            hash.put("sequence", 1);
//                            LR_MultiValue.add(hash);
                            localRefMult.add(hash);
//                            document.append(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), LR_MultiValue);
                            jsonObject.put(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), localRefMult);

                        } else if (Children.get(i).getAttribute("s") != null) {
                            if (Integer.valueOf(Children.get(i - 1).getAttributeValue("m")).equals(Integer.valueOf((Children.get(i).getAttributeValue("m"))))){
                                continue;
                            }else {
//                                LR_MultiValue = new ArrayList<>();
                                localRefMult = new ArrayList<>();
                            }


//                            hash.append(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), Children.get(i).getValue());
                            hash.put(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), Children.get(i).getValue());
//                            hash.append("sequence", Integer.valueOf(Children.get(i).getAttributeValue("s")));
                            hash.put("sequence", Integer.valueOf(Children.get(i).getAttributeValue("s")));
//                            LR_MultiValue.add(hash);
                            localRefMult.add(hash);
//                            document.append(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), LR_MultiValue);
                            jsonObject.put(usrFieldJs.get("m" + Children.get(i).getAttributeValue("m")).toString(), localRefMult);

                        }
                    }
                }
            }
        }
        return jsonObject;
    }
}
