package com.t24extractor.partitioner;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class MssqlPartitioner implements Partitioner {
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> result = new HashMap<>();

        int min = 0;

        int targetSize = gridSize - min;

        while (min < gridSize)
        {
            ExecutionContext value = new ExecutionContext();

            result.put("partition" + min, value);

            value.putInt("value", min);

            min++;
        }
        return result;
    }
}
