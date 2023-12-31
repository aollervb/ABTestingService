package com.aovsa.abtestingservice.requests;

import lombok.Data;

import java.util.HashMap;

@Data
public class ModifyVariationWeightRequest {
    private String experimentId;
    private HashMap<String, Double> variationWeights;

}
