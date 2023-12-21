package com.aovsa.abtestingservice.dtos;

import lombok.Data;

@Data
public class VariationDTO {
    private String id;
    private String variationName;
    private Double variationWeight;
}
