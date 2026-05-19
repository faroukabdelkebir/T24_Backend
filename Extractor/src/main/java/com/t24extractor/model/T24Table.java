package com.t24extractor.model;

import lombok.*;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class T24Table 
{
    @Id
    private String recid ;
    private String xmlRecord ;
}
