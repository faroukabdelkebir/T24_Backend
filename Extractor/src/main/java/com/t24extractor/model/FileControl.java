package com.t24extractor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("F_FILE_CONTROL")
public record FileControl(
        @Id
        @Column("RECID")
        String recid,

        @Column("SUFFIXES")
        String suffixes,

        @Column("CLASSIFICATION")
        String classification
){}
