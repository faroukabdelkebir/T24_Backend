package com.t24extractor.model;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import static com.t24extractor.commons.Constants.T24_METADATA;

@Table(value = T24_METADATA)
public record MetaDataTable(
        @Column("Table_Name") String tablename,
        @Column("MultiValueFields") String multivalueFields,
        @Column("LOCAL_REF") boolean localref) {
}
