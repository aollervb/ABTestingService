package com.aovsa.abtestingservice.controllers;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import com.aovsa.abtestingservice.dtos.VariationDTO;
import com.aovsa.abtestingservice.requests.CreateExperimentRequest;
import com.aovsa.abtestingservice.requests.ModifyVariationWeight;
import com.aovsa.abtestingservice.requests.VariationAssignmentRequest;
import com.aovsa.abtestingservice.responses.CreateExperimentResponse;
import com.aovsa.abtestingservice.services.ExperimentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/experiment")
public class ExperimentController {
    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/")
    public ResponseEntity<CreateExperimentResponse> createExperiment(@RequestBody CreateExperimentRequest request) {
        return experimentService.createExperiment(request);
    }

    @GetMapping("/")
    public ResponseEntity<List<ExperimentDTO>> getAllExperiments() {
        return experimentService.getAllExperiments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDTO> getExperimentById(@PathVariable String id) {
        return experimentService.getExperimentById(id);
    }

    @GetMapping("/assignment")
    public String getExperimentByAssignmentId(@RequestBody VariationAssignmentRequest request) {
        return experimentService.getVariationAssignment(request);
    }

    @PostMapping("/assignment")
    public ResponseEntity<List<VariationDTO>> modifyVariationWeights(@RequestBody ModifyVariationWeight request) {
        return experimentService.updateVariationWeightsForExperiment(request);
    }
}
