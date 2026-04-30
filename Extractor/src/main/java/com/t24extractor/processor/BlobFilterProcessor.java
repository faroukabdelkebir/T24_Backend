package com.t24extractor.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.HashSet;

public class BlobFilterProcessor implements ItemProcessor<HashMap<String, Object>, HashMap<String, Object>> 
{
    @Value("#{jobExecutionContext['IDS']}")
    HashSet<String> ids_list;

    @Override
    public HashMap<String, Object> process(HashMap<String, Object> item) throws Exception
    {
        if (ids_list.contains(item.get("_id")))
        {
            return null;
        } 
        else 
        {
            return item;
        }
    }
}
