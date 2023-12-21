package com.aovsa.abtestingservice.requests;

import lombok.Data;

@Data
public class CreateExperimentRequest {
    private String experimentName;
    private int variations;
    private String author;
}
