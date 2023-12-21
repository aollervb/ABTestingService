package com.aovsa.abtestingservice.services;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import com.aovsa.abtestingservice.dtos.VariationDTO;
import com.aovsa.abtestingservice.models.ExperimentModel;
import com.aovsa.abtestingservice.models.ExperimentVariationModel;
import com.aovsa.abtestingservice.repositories.ExperimentRepository;
import com.aovsa.abtestingservice.repositories.VariationsRepository;
import com.aovsa.abtestingservice.requests.CreateExperimentRequest;
import com.aovsa.abtestingservice.responses.CreateExperimentResponse;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.log4j.Log4j2;

import static java.lang.System.currentTimeMillis;

@Service
@Log4j2
public class ExperimentService {
    public static final String VARIATIONS_PREFIX = "V";
    public static final double STARTING_WEIGHT = 0;
    public static final String V_0 = "V0";
    private final ExperimentRepository experimentRepository;
    private final VariationsRepository variationsRepository;
    private final ModelMapper modelMapper;

    public ExperimentService(ExperimentRepository experimentRepository,
                             VariationsRepository variationsRepository, ModelMapper modelMapper) {
        this.experimentRepository = experimentRepository;
        this.variationsRepository = variationsRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Gets an experiment by its ID
     * @param id
     * @return ResponseEntity<ExperimentModel>
     */
    public ResponseEntity<ExperimentDTO> getExperimentById(String id) {
        long startOfRequest = currentTimeMillis();
        ExperimentModel model = experimentRepository.findById(id);

        if (model != null) {

            List<ExperimentVariationModel> variationModelList = new ArrayList<>();
            for (String variation : model.getVariations()) {
                ExperimentVariationModel variationModel = variationsRepository.findById(variation);
                variationModelList.add(variationModel);
            }

            ExperimentDTO experimentDTO = mapToDTO(model,variationModelList);
            long latency = currentTimeMillis() - startOfRequest;
            log.info("ExperimentService:ExperimentRetrieval:Latency:{}ms", latency);
            return new ResponseEntity<>(experimentDTO, HttpStatus.OK);
        }

        long latency = currentTimeMillis() - startOfRequest;
        log.error("Experiment with Id: {} was not found", id);
        log.info("ExperimentService:ExperimentRetrieval:Latency:{}ms", latency);

        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    /**
     * Gets all experiments
     * @return ResponseEntity<List<ExperimentModel>>
     */
    public ResponseEntity<List<ExperimentDTO>> getAllExperiments() {
        long startOfRequest = currentTimeMillis();
        List<ExperimentModel> experiments = (List<ExperimentModel>) experimentRepository.findAll();
        List<ExperimentDTO> experimentDTOS = new ArrayList<>();
        for (ExperimentModel experiment : experiments) {

            List<ExperimentVariationModel> variations = new ArrayList<>();
            for (String variationId : experiment.getVariations()) {
                variations.add(variationsRepository.findById(variationId));
            }
            ExperimentDTO experimentDTO = (mapToDTO(experiment, variations));
            experimentDTOS.add(experimentDTO);
        }

        long latency = currentTimeMillis() - startOfRequest;
        log.info("ExperimentService:ExperimentRetrieval:Latency:{}ms", latency);
        return new ResponseEntity<>(experimentDTOS, HttpStatus.OK);
    }

    /**
     * Creates an experiment based on the contents of {@link CreateExperimentRequest)
     * @param request
     * @return ResponseEntity<CreateExperimentResponse>
     */
    public ResponseEntity<CreateExperimentResponse> createExperiment(CreateExperimentRequest request) {
        long startOfRequest = currentTimeMillis();
        HashMap<String, Object> validation = validateCreateExperiment(request);

        // If the request is invalid, return a response with the error message
        if ((boolean) validation.get("hasError")) {
            long latency = currentTimeMillis() - startOfRequest;
            log.info("ExperimentService:ExperimentCreation:Latency:{}ms", latency);
            return new ResponseEntity<>(
                    new CreateExperimentResponse(
                            (boolean)validation.get("hasError"),
                            (String) validation.get("errorMessage"),
                            latency
                    ),
                    (HttpStatus) validation.get("httpStatus")
            );
        }

        // Create the experiment
        ExperimentModel experiment = new ExperimentModel();
        experiment.setId(UUID.randomUUID().toString());
        experiment.setExperimentName(request.getExperimentName());
        experiment.setAuthor(request.getAuthor());
        experiment.setVariations(new ArrayList<>());

        try {
            //Save the experiment to the DB
            experimentRepository.save(experiment);

            //Create the variations for the experiment
            createVariationsForExperiment(experiment.getId(), request.getVariations());
            long latency =  currentTimeMillis() - startOfRequest;
            return new ResponseEntity<>(
                    new CreateExperimentResponse(
                            (boolean)validation.get("hasError"),
                            (String) validation.get("errorMessage"),
                                  latency
                    ),
                    (HttpStatus) validation.get("httpStatus")
            );

        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException("Unable to create experiment");
        }
    }

    private void createVariationsForExperiment(String experimentId, int numberOfVariations) {
        List<String> variationIds = new ArrayList<>();

        for (int i = 0; i < numberOfVariations; i++) {
            ExperimentVariationModel variation = new ExperimentVariationModel();
            variation.setId(UUID.randomUUID().toString());
            variation.setVariationName(VARIATIONS_PREFIX + i);
            variation.setExperimentId(experimentId);
            variation.setVariationWeight(STARTING_WEIGHT);

            variationsRepository.save(variation);
            variationIds.add(variation.getId());
        }
        ExperimentModel experimentModel = experimentRepository.findById(experimentId);
        experimentModel.setVariations(variationIds);
        experimentRepository.update(experimentModel);
    }

    private HashMap<String, Object> validateCreateExperiment(CreateExperimentRequest request) {
        HashMap<String, Object> response = new HashMap<>();
        if (request == null) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Request is Null");
            return response;
        }

        // Assert that the experiment has a name
        if (request.getExperimentName().isEmpty()) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Experiment has no name and a name is required.");
            return response;
        }

        // Assert that the experiment has at least two variations
        if (request.getVariations() < 2) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Experiment has less than two variations and at least two variations are required.");
            return response;
        }

        // Assert that the experiment has an author
        if (request.getAuthor().isEmpty()) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Experiment has no author and an author is required.");
            return response;
        }

        response.put("httpStatus", HttpStatus.BAD_REQUEST);
        response.put("hasError", false);
        response.put("errorMessage", null);
        return response;
    }

    public String getVariationAssignment(String cid, String expId) {

        // Get experiment weight
        // If experiment doesn't exist, return V0
        ExperimentModel expModel = experimentRepository.findById(expId);

        if(expModel == null) {
            return V_0;
        }

        for (String varId : expModel.getVariations()) {
            ExperimentVariationModel varModel = variationsRepository.findById(varId);
            if (varModel.getVariationWeight() == 100) {
                return varModel.getVariationName();
            }
        }
        // Check if V0 is < 100%
        // If it is = 100%, return V0

        //TODO: Create table and repo for Experiment assignments

        // Is CID available?
        // If CID is available, look for treatment with CID and return
        // Else, Is SessionId available?
        // If it is available, look for treatment with SID and return

        //TODO: Create hashing function for assigning treatments

        // If the workflow is upto this point, this means that this SID or CID still don't have a variation assigned,
        // so here, calculate assignment for the SID or CID, store in DB and return the result.

        return null;
    }

    private ExperimentDTO mapToDTO(ExperimentModel experimentModel, List<ExperimentVariationModel> variationModel) {
        List<VariationDTO> variationDTOList = variationModel
                .stream()
                .map(variation -> modelMapper.map(variation, VariationDTO.class)).toList();

        ExperimentDTO experimentDTO = new ExperimentDTO();
        experimentDTO.setId(experimentModel.getId());
        experimentDTO.setExperimentName(experimentModel.getExperimentName());
        experimentDTO.setVariations(variationDTOList);
        experimentDTO.setAuthor(experimentModel.getAuthor());

        return experimentDTO;
    }

}
