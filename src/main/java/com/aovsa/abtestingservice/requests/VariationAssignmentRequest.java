package com.aovsa.abtestingservice.requests;

import lombok.Data;

@Data
public class VariationAssignmentRequest {
    private String experimentId;
    private String customerId;
    private String sessionId;
}
