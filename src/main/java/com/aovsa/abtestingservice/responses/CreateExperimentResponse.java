package com.aovsa.abtestingservice.responses;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateExperimentResponse extends BaseResponse{
    private ExperimentDTO experimentDTO;
}
