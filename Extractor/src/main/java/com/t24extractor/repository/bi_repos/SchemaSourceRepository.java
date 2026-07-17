package com.t24extractor.repository.bi_repos;

import com.t24extractor.commons.Constants;
import com.t24extractor.model.SchemaSource;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaSourceRepository extends CrudRepository<SchemaSource, String> {
    @Query(Constants.SCHEMA_TABLE)
    List<SchemaSource> findAllByTableName(@Param("tableName") String tableName);

    @Query(Constants.SCHEMA_COLUMN_TABLE)
    Optional<SchemaSource> findAllByTableNameAndColumnName(@Param("tableName") String tableName, @Param("columnName") String columName);


    @Query(Constants.SCHEMA_ENABLED)
    List<SchemaSource> findAll(@Param("tableName") String tableName);

    @Query(Constants.COUNT_ROWS_TABLE_SCHEMA_SOURCE)
    long countByTableName(@Param("tableName") String tableName);

    @Query(Constants.SCHEMA_ENABLED)
    List<SchemaSource> findSchemaSourceByTableNameAndEnabled(@Param("tableName") String tableName, @Param("enabled") boolean enabled);

    @Query(Constants.COUNT_ENABLED_ROWS_TABLE_SCHEMA_SOURCE)
    long countAllByTableName(@Param("tableName") String tableName);

    @Modifying
    @Query("UPDATE " + Constants.SCHEMA_SOURCE + " SET _enabled = :enabled WHERE table_name = :tableName AND column_name = :columnName")
    int updateEnabledByTableNameAndColumnName(
            @Param("tableName") String tableName,
            @Param("columnName") String columnName,
            @Param("enabled") boolean enabled
    );

}
