package com.aovsa.abtestingservice.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
public class VariationAssignmentResponse extends BaseResponse {
    String experimentId;
    String variationAssignment;
}
