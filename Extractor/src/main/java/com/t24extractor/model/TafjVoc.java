package com.t24extractor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("TAFJ_VOC")
public record TafjVoc(
        @Id
        @Column("RECID")
        String recid,
        @Column("ISBLOB")
        String isBlob,
        @Column("ORCLFILENAME")
        String orclFileName

) {
}
