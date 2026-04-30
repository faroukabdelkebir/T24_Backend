package com.t24extractor.repository.bi_repos;

import com.t24extractor.commons.Constants;
import com.t24extractor.model.SchemaSourceAudit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SchemaSourceAuditRepository extends CrudRepository<SchemaSourceAudit, Integer> {

    @Query(Constants.SCHEMA_AUDIT_TABLE)
    List<SchemaSourceAudit> findAllBySourceTableName(@Param("tableName") String tableName);

}
