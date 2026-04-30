package com.t24extractor.tasklet;

import com.t24extractor.repository.t24_repo.StandardSelectionRepository;
import com.t24extractor.repository.t24_repo.TafjVocRepository;
import lombok.extern.slf4j.Slf4j;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Element;
//import org.bson.Document;
import org.json.JSONObject;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;


@Slf4j
public class StandardSelectionTasklet implements Tasklet {

    /**
     * This is an example of xml document that we are going to treat
     * <p>
     * <row id='CUSTOMER'>
     * </p>
     * <p>
     * <c1>@ID</c1> -> system field's name
     * </p>
     * <p>
     * <c2>D</c2> -> system field
     * </p>
     * <p>
     * <c3 m='4'>1</c3> -> system field's number
     * </p>
     * <p>
     * <c4 m='3'>IN2CUS</c4> system field's data type
     * </p>
     * <p>
     * <c10 m='142'>M</c10> -> system field's type (multiple or single)
     * </p>
     * <p>
     * <c15 m='2'>AGR.DATE</c15> -> Local Ref's field name
     * </p>
     * <p>
     * <c17 m='2'>LOCAL.REF&lt;1,2&gt;</c17> -> Local Ref's number
     * </p>
     * <p>
     * <c18 m='2'>IN2A</c18> -> Local Ref's data type
     * </p>
     * <p>
     * <c24 m='2'>S</c24> -> Local Ref's type (multiple or single)
     * </p>
     * <p>
     * </row>
     * </p>
     */
    @Autowired
    @Qualifier("JdbcBI")
    private JdbcTemplate jdbcBatchExecutor;
    private JdbcOperations jdbcExecutor;
    private String oracleTableName;
    private String t24TableName;
    private String tableType;
    public static List<String> blobViewFields;
    public static HashSet<String> MultiValueBlobFields;
    private final static String BATCH_CONFIG_TABLE = "batch_config";
    private final static String ORACLENAMECOLUMN = "TableName";
    private final static String TYPECOLUMN = "Type";

    @Autowired
    private TafjVocRepository tafjVocRepository;

    @Autowired
    private StandardSelectionRepository standardSelectionRepository;

    protected JdbcOperations getJdbc_template() {
        return jdbcExecutor;
    }

    public void setJdbcExecutor(JdbcOperations jdbcOperations) {
        jdbcExecutor = jdbcOperations;
    }
    public void setOracleTableName(String oracleName) {
        oracleTableName = oracleName;
    }

    public void setT24TableName(String t24Name) {
        t24TableName = t24Name;
    }

    public void setTableType(String type) {
        tableType = type;
    }


    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
            throws Exception {
        blobViewFields = new ArrayList<>();

        MultiValueBlobFields = new HashSet<>();

        assert t24TableName != null;

        String tafjVocRow;

        tafjVocRow = tafjVocRepository.findTafjVocByOrclFileName(oracleTableName).orElseThrow(NullPointerException::new).isBlob();


        if (tafjVocRow != null && !tafjVocRow.isEmpty()) {
            if (!tableType.equals(tafjVocRow)) {
                String sqlQuery =
                        "UPDATE " + BATCH_CONFIG_TABLE + " SET \"" + TYPECOLUMN + "\" = '" + tafjVocRow
                                + "' " + "WHERE \"" + ORACLENAMECOLUMN + "\" = '" + oracleTableName + "'";

                jdbcBatchExecutor.execute(sqlQuery);
                chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                        .put("TYPE",
                                tafjVocRow);
            }
        }

        String xmlStr;
        if (t24TableName.contains("$HIS") || t24TableName.contains("$NAU") || t24TableName.contains(
                "$ARC")) {
            xmlStr = standardSelectionRepository.findById(t24TableName.substring(0, t24TableName.indexOf("$"))).orElseThrow(NullPointerException::new).getXml_record();
        } else {
            xmlStr = standardSelectionRepository.findById(t24TableName).orElseThrow(NullPointerException::new).getXml_record();
        }

        JSONObject standardSelectionJs = new JSONObject();

        JSONObject fieldNo = new JSONObject();
        JSONObject sysField = new JSONObject();
        JSONObject dataField = new JSONObject();
        JSONObject lengthField = new JSONObject();
        JSONObject nameField = new JSONObject();
        JSONObject typeField = new JSONObject();
        JSONObject enhancedFieldType = new JSONObject();
        JSONObject enhancedFieldDataType = new JSONObject();
        JSONObject usrField = new JSONObject();
        var sysFieldOds = new JSONObject();
        JSONObject BI_Standard_Selection = new JSONObject();
        JSONObject dataTypeField = new JSONObject();
        JSONObject usrFieldNo = new JSONObject();
        JSONObject usrFieldName = new JSONObject();
        JSONObject usrFieldtype = new JSONObject();
        JSONObject usrFieldDataType = new JSONObject();
        JSONObject usrFieldTypeOds = new JSONObject();
        JSONObject enhancedUsrFieldtype = new JSONObject();
        JSONObject enhancedUsrFieldDataType = new JSONObject();
        String localRefPos = "";

        /* Read xml into SAX stream binary and initialize the root element */
        final Builder parser = new Builder();
        final nu.xom.Document xmlRecord = parser.build(xmlStr, null);
        final Element element = xmlRecord.getRootElement();

        List<String> fields = new ArrayList<>();
        //  fields.add("FIELD_ID");

        /* Select FieldNo into a hashmap fieldNo */

        for (Element child : element.getChildElements()) {
            if (child.getAttributeCount() == 0) {
                child.addAttribute(new Attribute("m", "1"));
            }

            switch (child.getLocalName()) {
                case "c1":
                    if (!child.getValue().equals("@ID")) {
                        nameField.put(child.getAttributeValue("m"), child.getValue().replaceAll("\\.", "_"));
                    }
                case "c2":
                    if (child.getValue().equals("D")) {
                        dataField.put(child.getAttributeValue("m"), child.getValue());
                    }
                case "c3":
                    if (!child.getValue().equals("0")) {
                        fieldNo.put(child.getAttributeValue("m"), child.getValue());
                    }
                case "c4":
                    dataTypeField.put(child.getAttributeValue("m"), child.getValue());
                case "c6":
                    lengthField.put(child.getAttributeValue("m"), child.getValue());
                case "c10":
                    typeField.put(child.getAttributeValue("m"), child.getValue());
                case "c15":
                    usrFieldName.put(child.getAttributeValue("m"), child.getValue().replaceAll("\\.", "_"));
                case "c17":
                    if (child.getValue().contains(",") && child.getValue().contains(">")) {
                        usrFieldNo.put(child.getAttributeValue("m"), child.getValue()
                                .substring(child.getValue().indexOf(',') + 1, child.getValue().indexOf('>')));
                    }
                case "c18":
                    if (child.getValue().contains("&")) {
                        usrFieldDataType.put(child.getAttributeValue("m"),
                                child.getValue().substring(0, child.getValue().indexOf("&")));
                    } else {
                        usrFieldDataType.put(child.getAttributeValue("m"), child.getValue());
                    }
                case "c24":
                    usrFieldtype.put(child.getAttributeValue("m"), child.getValue());
            }

            HashMap<String, String> auxMap = new HashMap<>();
            Set<String> field2keys = dataField.keySet();

            field2keys.retainAll(fieldNo.keySet());

            for (String key : field2keys) {
                auxMap.put(key, (String) fieldNo.get(key));
            }

            Set<String> auxmapKey = auxMap.keySet();
            auxmapKey.retainAll(nameField.keySet());

            for (String name : auxmapKey) {
                sysField.put("c" + auxMap.get(name), nameField.get(name));
            }

            Set<String> tfkey = typeField.keySet();
            tfkey.retainAll(auxMap.keySet());

            for (String type : tfkey) {
                enhancedFieldType.put("c" + auxMap.get(type), typeField.get(type));
            }

            auxmapKey.retainAll(nameField.keySet());

            for (String name : auxmapKey) {
                if (typeField.get(name).equals("S")) {
                    if (dataTypeField.get(name).equals("IN2BMT") ||
                            dataTypeField.get(name).equals("IN2AMT") ||
                            dataTypeField.get(name).toString().contains("IN2AMT&") ||
                            dataTypeField.get(name).equals("IN2BMT") ||
                            dataTypeField.get(name).toString().contains("IN2BMT&") ||
                            dataTypeField.get(name).equals("IN2CCYDEC") ||
                            dataTypeField.get(name).toString().contains("IN2CCYDEC&") ||
                            dataTypeField.get(name).equals("IN2DEC") ||
                            dataTypeField.get(name).toString().contains("IN2DEC&") ||
                            dataTypeField.get(name).equals("IN2LAMT") ||
                            dataTypeField.get(name).toString().contains("IN2LAMT&") ||
                            dataTypeField.get(name).equals("IN2NOMAMT") ||
                            dataTypeField.get(name).toString().contains("IN2NOMAMT&") ||
                            dataTypeField.get(name).equals("IN2R.AMT") ||
                            dataTypeField.get(name).toString().contains("IN2R.AMT&") ||
                            dataTypeField.get(name).equals("IN2TAX") ||
                            dataTypeField.get(name).toString().contains("IN2TAX&") ||
                            dataTypeField.get(name).equals("IN2FWDR") ||
                            dataTypeField.get(name).toString().contains("IN2FWDR&") ||
                            dataTypeField.get(name).equals("IN2PRICE") ||
                            dataTypeField.get(name).toString().contains("IN2PRICE&") ||
                            dataTypeField.get(name).equals("IN2R") ||
                            dataTypeField.get(name).toString().contains("IN2R&") ||
                            dataTypeField.get(name).equals("IN2RR") ||
                            dataTypeField.get(name).toString().contains("IN2RR&")) {
                        sysFieldOds.put((String) nameField.get(name), "Float"+"."+"c" + name);

                    } else if (dataTypeField.get(name).toString().contains("IN2.D&") ||
                            dataTypeField.get(name).toString().contains("IN2.YM&") ||
                            dataTypeField.get(name).equals("IN2.D") ||
                            dataTypeField.get(name).equals("IN2.YM")) {
                        sysFieldOds.put((String) nameField.get(name), "Integer"+"."+"c" + name);
                    } else {
                        sysFieldOds.put((String) nameField.get(name),
                                "String" + lengthField.get(name).toString()+"."+"c" + name);
                    }
                } else {
                    sysFieldOds.put((String) nameField.get(name), "String" + "MAX"+"."+"c" + name);
                    MultiValueBlobFields.add((String) nameField.get(name));
                }
            }

            auxmapKey.retainAll(dataTypeField.keySet());

            for (String dTypeField : field2keys) {
                if (dataTypeField.get(dTypeField).equals("IN2BMT") ||
                        dataTypeField.get(dTypeField).equals("IN2AMT") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2AMT&") ||
                        dataTypeField.get(dTypeField).equals("IN2BMT") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2BMT&") ||
                        dataTypeField.get(dTypeField).equals("IN2CCYDEC") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2CCYDEC&") ||
                        dataTypeField.get(dTypeField).equals("IN2DEC") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2DEC&") ||
                        dataTypeField.get(dTypeField).equals("IN2LAMT") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2LAMT&") ||
                        dataTypeField.get(dTypeField).equals("IN2NOMAMT") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2NOMAMT&") ||
                        dataTypeField.get(dTypeField).equals("IN2R.AMT") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2R.AMT&") ||
                        dataTypeField.get(dTypeField).equals("IN2TAX") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2TAX&") ||
                        dataTypeField.get(dTypeField).equals("IN2FWDR") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2FWDR&") ||
                        dataTypeField.get(dTypeField).equals("IN2PRICE") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2PRICE&") ||
                        dataTypeField.get(dTypeField).equals("IN2R") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2R&") ||
                        dataTypeField.get(dTypeField).equals("IN2RR") ||
                        dataTypeField.get(dTypeField).toString().contains("IN2RR&")) {
                    enhancedFieldDataType.put("c" + auxMap.get(dTypeField), "Float");
                } else if (dataTypeField.get(dTypeField).toString().contains("IN2.D&") || dataTypeField.get(
                        dTypeField).toString().contains("IN2.YM&") || dataTypeField.get(dTypeField)
                        .equals("IN2.D") || dataTypeField.get(dTypeField).equals("IN2.YM")) {
                    enhancedFieldDataType.put("c" + auxMap.get(dTypeField), "Integer");
                } else if (dataTypeField.get(dTypeField).toString().contains("TEXT")) {
                    enhancedFieldType.put("c" + auxMap.get(dTypeField), "M");
                } else {
                    enhancedFieldDataType.put("c" + auxMap.get(dTypeField), "String");
                }
            }

            // Traitement des champs local Ref
            Set<String> usrfield2keys = usrFieldNo.keySet();
            usrfield2keys.retainAll(usrFieldName.keySet());

            for (String key : usrfield2keys) {
                usrField.put("m" + usrFieldNo.get(key), usrFieldName.get(key));
            }

            usrfield2keys.retainAll(usrFieldtype.keySet());

            for (String type : usrfield2keys) {
                enhancedUsrFieldtype.put("m" + usrFieldNo.get(type), usrFieldtype.get(type));
                usrFieldTypeOds.put(usrField.get("m" + usrFieldNo.get(type)).toString(),
                        usrFieldtype.get(type));
            }

            usrfield2keys.retainAll(usrFieldDataType.keySet());

            for (String dTypeField : usrfield2keys) {
                if (usrFieldDataType.get(dTypeField).equals("IN2AMT") ||
                        usrFieldDataType.get(dTypeField).equals("IN2BMT") ||
                        usrFieldDataType.get(dTypeField).equals("IN2CCYDEC") ||
                        usrFieldDataType.get(dTypeField).equals("IN2DEC") ||
                        usrFieldDataType.get(dTypeField).equals("IN2LAMT") ||
                        usrFieldDataType.get(dTypeField).equals("IN2NOMAMT") ||
                        usrFieldDataType.get(dTypeField).equals("IN2R.AMT") ||
                        usrFieldDataType.get(dTypeField).equals("IN2TAX") ||
                        usrFieldDataType.get(dTypeField).equals("IN2FWDR") ||
                        usrFieldDataType.get(dTypeField).equals("IN2PRICE") ||
                        usrFieldDataType.get(dTypeField).equals("IN2R") ||
                        usrFieldDataType.get(dTypeField).equals("IN2RR")) {
                    enhancedUsrFieldDataType.put("m" + usrFieldNo.get(dTypeField), "Float");
                } else {
                    enhancedUsrFieldDataType.put("m" + usrFieldNo.get(dTypeField), "String");
                }
            }

            for (Map.Entry pos : sysField.toMap().entrySet()) {
                if (pos.getValue().equals("LOCAL_REF")) {
                    localRefPos = pos.getKey().toString();
                }
            }
        }

        StringBuilder Local_ref_fields = new StringBuilder();
        Iterator<Map.Entry<String, Object>> usr_iterator = usrField.toMap().entrySet().iterator();

        while (usr_iterator.hasNext()) {
            Map.Entry<String, Object> entry = usr_iterator.next();

            Local_ref_fields.append("t.").append(entry.getValue().toString());
            blobViewFields.add(entry.getValue().toString());

            if (usr_iterator.hasNext()) {
                Local_ref_fields.append(", ");
            }
        }

        enhancedFieldType.remove("cD");

        Iterator<Map.Entry<String, Object>> iterator = enhancedFieldType.toMap().entrySet().iterator();

        if (!"X".equalsIgnoreCase(tafjVocRow)) {
            StringBuilder view_query = new StringBuilder("SELECT RECID as FIELD_ID, ");

            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();

                if (!entry.getKey().equals(localRefPos)) {
                    view_query.append(sysField.get(entry.getKey()).toString());
                    blobViewFields.add(sysField.get(entry.getKey()).toString());

                    if (iterator.hasNext()) {
                        view_query.append(", ");
                    }
                }

            }
            if (!Local_ref_fields.isEmpty()) {
                view_query.append(", ");
                view_query.append(Local_ref_fields);
            }

            view_query.append(", ROWNUM rowNumber FROM ").append("V_").append(oracleTableName)
                    .append(" t");

            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .put("VIEW_QUERY",
                            view_query.toString());

        }

        BI_Standard_Selection.put("sysFieldOds", sysFieldOds);

//        Document document = new Document();

        standardSelectionJs.append("sysField", sysField);
        standardSelectionJs.append("typeField", enhancedFieldType);
        standardSelectionJs.append("DataTypeField", enhancedFieldDataType);
        standardSelectionJs.append("usrField", usrField);
        standardSelectionJs.append("UsrFieldType", enhancedUsrFieldtype);
        standardSelectionJs.append("UsrFieldDataType", enhancedUsrFieldDataType);
        standardSelectionJs.append("localRefPos", localRefPos);



        usrField.toMap().forEach((k, v) ->
        {
            if (enhancedUsrFieldtype.get(k).equals("M")) {
                sysFieldOds.put(v.toString(), "Local_Ref_StringMAX"+"."+k);
                MultiValueBlobFields.add(v.toString());

            } else {
                sysFieldOds.put(v.toString(), "Local_Ref_String"+"."+k);
            }
        });

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("Mongo_Standard_Selection", standardSelectionJs.toString());

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("fields", fields);

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("sysFieldOds", sysFieldOds.toString());

//        document.append("_id", t24TableName.replaceAll("\\.", "_").replace("$", "#"));
//        document.append("SCHEMA", Document.parse(String.valueOf(sysFieldOds)));
//        mongoExecutor.save(document, "SS_QUERY_BI");

        return RepeatStatus.FINISHED;
    }

}
