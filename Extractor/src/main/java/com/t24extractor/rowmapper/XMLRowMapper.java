package com.t24extractor.rowmapper;

import com.t24extractor.model.T24Table;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class XMLRowMapper implements RowMapper<T24Table> 
{
    @Override
    public T24Table mapRow(ResultSet resultSet, int i) throws SQLException 
    {
    	T24Table t24XMLItem = new T24Table();
        
    	t24XMLItem.setRecid(resultSet.getString(1));
    	t24XMLItem.setXml_record(resultSet.getString(2));

        return(t24XMLItem) ;
    }
}
