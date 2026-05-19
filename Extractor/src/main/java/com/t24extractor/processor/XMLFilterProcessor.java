package com.t24extractor.processor;

import com.t24extractor.model.T24Table;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;


public class XMLFilterProcessor implements ItemProcessor<T24Table, T24Table> 
{
    @Value("#{jobExecutionContext['IDS']}")
    HashSet<String> idsList;

    @Override
    public T24Table process(T24Table item) throws Exception 
    {
        if (idsList.contains(item.getRecid()))
        {
            return null;
        } 
        else 
        {
            return item;
        }
    }
}