package com.t24extractor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import static com.t24extractor.commons.Constants.SCHEMA_SOURCE_AUDIT;

@Table(SCHEMA_SOURCE_AUDIT)
public record SchemaSourceAudit(
        @Id
        @Column("id")
        int id,

        @Column("sourceId")
        String sourceId,

        @Column("sourceTable_name")
        String sourceTableName,

        @Column("sourceColumn_name")
        String ColumnName,
        @Column("sourceDataType")
        String dataType,
        @Column("newSource_enabled")
        int newSourceEnabled,
        @Column("oldSource_enabled")
        int oldSourceEnabled,

        @Column("newSourceIndexed")
        int newSourceIndexed,
        @Column("oldSourceIndexed")
        int oldSourceIndexed

) {
}
