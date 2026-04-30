package com.t24extractor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("F_TSA_SERVICE")
public record CobStatus(
        @Id
        @Column("RECID")
        String recid,
        @Column("SERVICE_CONTROL")
        String serviceControl
) {
}
