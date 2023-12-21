package com.aovsa.abtestingservice.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ExperimentDTO {
    private String id;
    private String experimentName;
    private List<VariationDTO> variations;
    private String author;


}
