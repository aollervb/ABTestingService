package com.aovsa.abtestingservice.responses;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModifyVariationWeightResponse extends BaseResponse {
    private ExperimentDTO experimentDTO;
}
