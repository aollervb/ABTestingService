package com.aovsa.abtestingservice.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetVariationAssignmentResponse extends BaseResponse {
    String experimentId;
    String variationAssignment;
}
