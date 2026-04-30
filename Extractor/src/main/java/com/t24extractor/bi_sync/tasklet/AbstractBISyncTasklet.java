package com.t24extractor.bi_sync.tasklet;


import com.t24extractor.model.SchemaSource;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface AbstractBISyncTasklet {

    boolean statusSchemaSource(String tableName);

    void loadMetaDataTable(String tableName, List<SchemaSource> schemaSourceList);

    void checkSchemaUpdate(String tableName);

    void scheduleOp(String tableName);

    void updateIndex(String tableName, int diff, String column);


    void updateColumn(String table, int diff, String column, String dataType);

    void loadSchemaSource(String tableName, JSONObject ss);

    JSONObject ssMapClean(String tableName, JSONObject ssMap);

}
