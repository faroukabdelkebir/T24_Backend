package com.t24extractor.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;

@Table("F_STANDARD_SELECTION")
@Data
public class StandardSelectionRecord implements Serializable {
        @Id
        @Column("RECID")
        private String recid ;
        @Column("XMLRECORD")
        private String xmlRecord ;
}
