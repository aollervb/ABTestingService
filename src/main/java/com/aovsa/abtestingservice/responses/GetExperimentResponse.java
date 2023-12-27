package com.aovsa.abtestingservice.responses;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class GetExperimentResponse extends BaseResponse{
    public List<ExperimentDTO> experiments;
}
