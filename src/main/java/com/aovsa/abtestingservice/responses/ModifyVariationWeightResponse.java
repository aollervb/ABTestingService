package com.aovsa.abtestingservice.responses;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import com.aovsa.abtestingservice.dtos.VariationDTO;
import lombok.Data;

import java.util.List;
@Data
public class ModifyVariationWeightResponse extends BaseResponse {
    private ExperimentDTO experimentDTO;
}
