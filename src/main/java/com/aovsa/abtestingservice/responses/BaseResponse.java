package com.aovsa.abtestingservice.responses;

import lombok.Data;

@Data
public class BaseResponse {
    private boolean hasError;
    private String error;
    private long requestLatency;
}
