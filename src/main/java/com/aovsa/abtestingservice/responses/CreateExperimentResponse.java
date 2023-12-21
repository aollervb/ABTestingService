package com.aovsa.abtestingservice.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateExperimentResponse {
    private Boolean hasError;
    private String errorMessage;
    private long requestLatency;
}
