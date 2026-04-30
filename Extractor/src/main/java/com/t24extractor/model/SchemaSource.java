package com.t24extractor.model;

import jakarta.annotation.Generated;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import static com.t24extractor.commons.Constants.SCHEMA_SOURCE;


@Table(value = SCHEMA_SOURCE)
@Data
@AllArgsConstructor
public class SchemaSource implements Persistable<String> {

    @Id
    String id;

    @Column("table_name")
    String tableName;

    @Column("column_name")
    String columnName;

    @Column("datatype")
    String dataType;

    @Column("_enabled")
    boolean enabled;

    @Column("indexed")
    boolean indexed;

    @Column("ssTag")
    String ssTag;

    @Column("single_mult")
    String singleMult;

    @Column("local_ref")
    boolean localRef;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }

}
