package com.t24extractor.PreparedStatementSetter;

import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
//import org.bson.Document;
import org.springframework.batch.infrastructure.item.database.ItemPreparedStatementSetter;
import org.springframework.beans.factory.annotation.Value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ItemPrepStmtSetter implements ItemPreparedStatementSetter<JSONObject> {

    @Value("#{jobExecutionContext['COLUMNS']}")
    List<String> columns;

    Gson gson = new Gson();
    @Override
    public void setValues(JSONObject document, PreparedStatement preparedStatement) throws SQLException {

            int i = 1;
            for (String key: columns) {
                if (document.get(key) != null){
                    if (document.get(key) instanceof List){
                        preparedStatement.setString(i, gson.toJson(document.get(key)));
                    }else {
                        preparedStatement.setObject(i, document.getOrDefault(key, Object.class));
                    }
                } else {
                    preparedStatement.setString(i, null);
                }
                i++;
            }

        }
}
