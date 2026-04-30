package com.t24extractor.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class Job {
	private Long jobId ;
    private String status ;
    private String duration ;
    private String name ;
    private Integer read ;
    private Integer write ;
}
