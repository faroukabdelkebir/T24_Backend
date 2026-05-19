package com.t24extractor.model;

import lombok.*;
import org.springframework.stereotype.Component;

@Data
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Component
public class TableMessage 
{
    private Long id;
    
    private String tableName ;
    
    private String t24SelectionName ;
    
    private String chargement ;
    
    private String colonneRef ;
    
    private String lastDate ;
    
    private String indexes ;
    
    private String type ;
    
    private String destination ;
    
    private String dateFormat ;
    
    private String futureDate ;
    
    private String runID ;
}
