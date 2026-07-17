package com.t24extractor.controller;

import com.t24extractor.model.UpdateSchemaSourceEnabledRequest;
import com.t24extractor.exception.DataNotFoundException;
import com.t24extractor.model.SchemaSource;
import com.t24extractor.repository.bi_repos.SchemaSourceRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-source")
public class SchemaSourceController {

    private final SchemaSourceRepository schemaSourceRepository;

    public SchemaSourceController(SchemaSourceRepository schemaSourceRepository) {
        this.schemaSourceRepository = schemaSourceRepository;
    }

    @GetMapping("/{tableName}")
    public List<SchemaSource> getColumnsByTable(@PathVariable String tableName) {
        return schemaSourceRepository.findAllByTableName(tableName);
    }

    @PatchMapping("/column/enabled")
    public SchemaSource updateColumnEnabled(
            @RequestParam String tableName,
            @RequestParam String columnName,
            @RequestBody UpdateSchemaSourceEnabledRequest request
    ) throws DataNotFoundException {
        int updatedRows = schemaSourceRepository.updateEnabledByTableNameAndColumnName(
                tableName,
                columnName,
                request.enabled()
        );
        if (updatedRows == 0) {
            throw new DataNotFoundException("Schema source column not found: " + tableName + "." + columnName);
        }
        return schemaSourceRepository.findAllByTableNameAndColumnName(tableName, columnName)
                .orElseThrow(() -> new DataNotFoundException("Schema source column not found: " + tableName + "." + columnName));
    }
}
