package com.t24extractor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import static com.t24extractor.commons.Constants.SCHEDULED_OPS;

@Table(value = SCHEDULED_OPS)
@Data
@AllArgsConstructor
public class ScheduledOps {
    @Id
    Long id;
    String table;
    String column;
    String op;
}
