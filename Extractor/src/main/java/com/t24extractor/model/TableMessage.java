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
    
    private String TableName ;
    
    private String T24SelectionName ;
    
    private String Chargement ;
    
    private String Colonne_Ref ;
    
    private String Last_Date ;
    
    private String Indexes ;
    
    private String Type ;
    
    private String Destination ;
    
    private String Date_Format ;
    
    private String Future_Date ;
    
    private String Run_ID ;
}
