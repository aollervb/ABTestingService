package com.aovsa.abtestingservice.services;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import com.aovsa.abtestingservice.dtos.VariationDTO;
import com.aovsa.abtestingservice.models.ExperimentModel;
import com.aovsa.abtestingservice.models.ExperimentVariationModel;
import com.aovsa.abtestingservice.repositories.ExperimentRepository;
import com.aovsa.abtestingservice.repositories.VariationsRepository;
import com.aovsa.abtestingservice.requests.CreateExperimentRequest;
import com.aovsa.abtestingservice.requests.ModifyVariationWeight;
import com.aovsa.abtestingservice.requests.VariationAssignmentRequest;
import com.aovsa.abtestingservice.responses.CreateExperimentResponse;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;

import static java.lang.System.currentTimeMillis;

@Service
@Log4j2
public class ExperimentService {
    public static final String VARIATIONS_PREFIX = "V";
    public static final double STARTING_WEIGHT = 0;
    public static final String V_0 = "V0";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private final ExperimentRepository experimentRepository;
    private final VariationsRepository variationsRepository;
    private final ModelMapper modelMapper;
    //TODO: Add logging
    //TODO: Document methods
    //TODO: Add unit tests
    //TODO: Add authentication with API key
    public ExperimentService(ExperimentRepository experimentRepository,
                             VariationsRepository variationsRepository,
                             ModelMapper modelMapper) {
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
        log.info("experimentService:getAllExperiments:latency:{}ms", latency);
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
            log.error("experimentService:experimentCreation:error:{}", validation.get("errorMessage"));
            log.info("experimentService:experimentCreation:latency:{}ms", latency);
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
            log.info("experimentService:experimentCreation:success");
            log.info("experimentService:experimentCreation:latency:{}ms", latency);
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
    /**
     * Updates the weights of the variations for a given experiment
     * @param request
     * @return ResponseEntity<List<VariationDTO>>
     */
    public ResponseEntity<List<VariationDTO>> updateVariationWeightsForExperiment(ModifyVariationWeight request) {
        if (!validateModifyVariations(request)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        ExperimentModel experimentModel = experimentRepository.findById(request.getExperimentId());
        if (experimentModel == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        List<VariationDTO> variationDTOList = new ArrayList<>();
        for(String var : experimentModel.getVariations()) {
            ExperimentVariationModel varModel = variationsRepository.findById(var);
            HashMap<String, Double> variationWeights = request.getVariationWeights();
            if (variationWeights.get(varModel.getVariationName()) != null) {
                varModel.setVariationWeight(variationWeights.get(varModel.getVariationName()));
                variationsRepository.update(varModel);
                variationDTOList.add(modelMapper.map(varModel, VariationDTO.class));
            }
        }

        return new ResponseEntity<>(variationDTOList, HttpStatus.OK);
    }

    /**
     * Gets the variation assignment for a given experiment
     * @param request
     * @return String
     */
    public String getVariationAssignment(VariationAssignmentRequest request) {
        long startOfRequest = currentTimeMillis();
        // Get experiment
        ExperimentModel expModel = experimentRepository.findById(request.getExperimentId());

        // If experiment doesn't exist, return V0
        if(expModel == null) {
            return V_0;
        }

        for (String varId : expModel.getVariations()) {
            ExperimentVariationModel varModel = variationsRepository.findById(varId);
            if (varModel.getVariationWeight() == 100 && varModel.getVariationName().equals(V_0)) {
                // If it V0 weight = 100%, return V0
                return varModel.getVariationName();
            }
        }

        return bucketing(expModel, request.getCustomerId(), request.getSessionId());
    }

    private String bucketing(ExperimentModel experimentModel, String customerId, String sessionId) {

        int numberOfVariations = experimentModel.getVariations().size();
        if (customerId != null && sessionId != null) {
            return V_0;
        }

        String stringToHash = experimentModel.getExperimentName();
        if (customerId != null && !customerId.isEmpty()) {
            stringToHash += customerId;
        } else if (sessionId != null && !sessionId.isEmpty()) {
            stringToHash += sessionId;
        }

        double hashValue =  (double) Integer.parseInt(hash(stringToHash).substring(0,5), 16) / 1000000;
        String variationId = experimentModel.getVariations().get(numberOfVariations - 1);
        ExperimentVariationModel varModel = variationsRepository.findById(variationId);
        if (hashValue <= varModel.getVariationWeight()/100) {
            return varModel.getVariationName();
        }

        return V_0;
    }

    private String hash(String stringToHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(stringToHash.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            char[] hexChars = new char[digest.length * 2];
            for (int j = 0; j < digest.length; j++) {
                int v = digest[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to hash string");
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

    private boolean validateModifyVariations(ModifyVariationWeight request) {
        if (request == null) {
            return false;
        }

        if (request.getExperimentId().isEmpty()) {
            return false;
        }

        if (request.getVariationWeights().isEmpty()) {
            return false;
        }
        double totalWeightOfExperiment = 0;
        for (Double weight : request.getVariationWeights().values()) {
            totalWeightOfExperiment += weight;
            if (totalWeightOfExperiment > 100) {
                return false;
            }
        }

        return true;
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
