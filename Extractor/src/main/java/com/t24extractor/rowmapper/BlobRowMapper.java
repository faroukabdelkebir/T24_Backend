package com.t24extractor.rowmapper;

import com.t24extractor.decider.ExecutionFlowDecider;
import com.t24extractor.tasklet.StandardSelectionTasklet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class BlobRowMapper implements RowMapper<HashMap<String, Object>>
{


    private static final Logger LOG = LoggerFactory.getLogger(ExecutionFlowDecider.class) ;

    @Override
    public HashMap<String, Object> mapRow(ResultSet resultSet, int i) throws SQLException 
    {
        HashMap<String, Object> t24BlobItem = new HashMap<>();
        t24BlobItem.put("_id", resultSet.getString("FIELD_ID"));
        for (String field : StandardSelectionTasklet.blobViewFields)
        {
        	try
        	{
        		t24BlobItem.put(field, resultSet.getObject(field));
        	}
        	catch(Exception ex)
        	{
        		LOG.trace("[BlobRowMapper] Erreur de mapping : " + field);
        	}    	
        }
        
        t24BlobItem.put("rowNumber", resultSet.getLong("rowNumber"));
        
        return(t24BlobItem) ;
    }
}
