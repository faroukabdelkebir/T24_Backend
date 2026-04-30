package com.t24extractor.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;


public class ColumnRangePartitioner implements Partitioner
{

    @Value("#{jobExecutionContext['COUNT']}")
    int count;
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize)
    {
        int min = 1;
        int max = count;
        int targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();
        
        int number = 0;
        int start = min;
        int end = start + targetSize - 1;

        while (start <= max)
        {
            ExecutionContext value = new ExecutionContext();
            
            result.put("partition" + number, value);

            if (end >= max)
            {
                end = max;
            }
            
            value.putInt("minValue", start);
            value.putInt("maxValue", end);
            
            start += targetSize;
            end += targetSize;
            
            number++;
        }

        return result;
    }
}
