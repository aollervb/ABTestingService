package com.aovsa.abtestingservice.services;

import com.aovsa.abtestingservice.dtos.ExperimentDTO;
import com.aovsa.abtestingservice.dtos.VariationDTO;
import com.aovsa.abtestingservice.models.ExperimentModel;
import com.aovsa.abtestingservice.models.ExperimentVariationModel;
import com.aovsa.abtestingservice.repositories.ExperimentRepository;
import com.aovsa.abtestingservice.repositories.VariationsRepository;
import com.aovsa.abtestingservice.requests.CreateExperimentRequest;
import com.aovsa.abtestingservice.requests.ModifyVariationWeightRequest;
import com.aovsa.abtestingservice.requests.VariationAssignmentRequest;
import com.aovsa.abtestingservice.responses.CreateExperimentResponse;

import com.aovsa.abtestingservice.responses.GetExperimentResponse;
import com.aovsa.abtestingservice.responses.VariationAssignmentResponse;
import com.aovsa.abtestingservice.responses.ModifyVariationWeightResponse;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
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
     * @param id Identificator for the Experiment.
     * @return ResponseEntity<ExperimentModel>
     */
    public ResponseEntity<GetExperimentResponse> getExperimentById(String id) {
        long startOfRequest = currentTimeMillis();
        ExperimentModel model = experimentRepository.findById(id);
        GetExperimentResponse response = new GetExperimentResponse();
        if (model != null) {
            List<ExperimentVariationModel> variationModelList = new ArrayList<>();
            for (String variation : model.getVariations()) {
                ExperimentVariationModel variationModel = variationsRepository.findById(variation);
                variationModelList.add(variationModel);
            }

            ExperimentDTO experimentDTO = mapToDTO(model,variationModelList);
            long latency = currentTimeMillis() - startOfRequest;
            log.info("ExperimentService:ExperimentRetrieval:Latency:{}ms", latency);

            response.setExperiments(List.of(experimentDTO));
            response.setHasError(false);
            response.setError(null);
            response.setRequestLatency(latency);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setExperiments(null);
        response.setHasError(true);
        response.setError(String.format("Experiment with Id: %s was not found", id));
        long latency = currentTimeMillis() - startOfRequest;
        response.setRequestLatency(latency);

        log.error("Experiment with Id: {} was not found", id);
        log.info("ExperimentService:ExperimentRetrieval:Latency:{}ms", latency);

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Gets all experiments
     * @return ResponseEntity<List<ExperimentModel>>
     */
    public ResponseEntity<GetExperimentResponse> getAllExperiments() {
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
        GetExperimentResponse response = new GetExperimentResponse();
        response.setExperiments(experimentDTOS);
        response.setHasError(false);
        response.setError(null);

        long latency = currentTimeMillis() - startOfRequest;
        response.setRequestLatency(latency);
        log.info("experimentService:getAllExperiments:latency:{}ms", latency);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

            CreateExperimentResponse response = CreateExperimentResponse.builder()
                    .experimentDTO(null)
                    .build();
            response.setHasError((Boolean) validation.get("hasError"));
            response.setError((String) validation.get("errorMessage"));
            response.setRequestLatency(latency);

            return new ResponseEntity<>(
                    response,
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
            List<ExperimentVariationModel> variationModelList =
                    createVariationsForExperiment(experiment.getId(), request.getVariations());
            long latency =  currentTimeMillis() - startOfRequest;
            log.info("experimentService:experimentCreation:success");
            log.info("experimentService:experimentCreation:latency:{}ms", latency);

            CreateExperimentResponse response = CreateExperimentResponse.builder()
                    .experimentDTO(mapToDTO(experiment, variationModelList))
                    .build();
            response.setHasError((Boolean) validation.get("hasError"));
            response.setError((String) validation.get("errorMessage"));
            response.setRequestLatency(latency);

            return new ResponseEntity<>(
                    response,
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
    public ResponseEntity<ModifyVariationWeightResponse> updateVariationWeightsForExperiment(ModifyVariationWeightRequest request) {
        long startOfRequest = currentTimeMillis();

        HashMap<String, Object> validation = validateModifyVariations(request);
        if ((boolean) validation.get("hasError")) {
            ModifyVariationWeightResponse response = new ModifyVariationWeightResponse();
            response.setExperimentDTO(null);
            response.setHasError((boolean) validation.get("hasError"));
            response.setError((String) validation.get("errorMessage"));
            long latency = currentTimeMillis() - startOfRequest;
            response.setRequestLatency(latency);
            log.info("experimentService:variationModification:error:{}", validation.get("errorMessage"));
            log.info("experimentService:experimentCreation:latency:{}ms", latency);
            return new ResponseEntity<>(response, (HttpStatus) validation.get("httpStatus"));
        }

        ExperimentModel experimentModel = experimentRepository.findById(request.getExperimentId());
        if (experimentModel == null) {
            ModifyVariationWeightResponse response = new ModifyVariationWeightResponse();
            response.setExperimentDTO(null);
            response.setHasError((boolean) validation.get("hasError"));
            response.setError((String) validation.get("errorMessage"));
            long latency = currentTimeMillis() - startOfRequest;
            response.setRequestLatency(latency);

            log.info("experimentService:variationModification:error:{}", validation.get("errorMessage"));
            log.info("experimentService:experimentCreation:latency:{}ms", latency);
            return new ResponseEntity<>(response, (HttpStatus) validation.get("httpStatus"));
        }

        List<ExperimentVariationModel> variationList = new ArrayList<>();
        for(String var : experimentModel.getVariations()) {
            ExperimentVariationModel varModel = variationsRepository.findById(var);
            HashMap<String, Double> variationWeights = request.getVariationWeights();
            if (variationWeights.get(varModel.getVariationName()) != null) {
                varModel.setVariationWeight(variationWeights.get(varModel.getVariationName()));
                variationsRepository.update(varModel);
                variationList.add(varModel);
            }
        }

        long latency = currentTimeMillis() - startOfRequest;
        ModifyVariationWeightResponse response = new ModifyVariationWeightResponse();
        response.setExperimentDTO(mapToDTO(experimentModel, variationList));
        response.setRequestLatency(latency);
        response.setHasError((boolean) validation.get("hasError"));
        response.setError(null);

        log.info("experimentService:variationModification:success");
        log.info("experimentService:experimentCreation:latency:{}ms", latency);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Gets the variation assignment for a given experiment
     *
     * @param request
     * @return String
     */
    public ResponseEntity<VariationAssignmentResponse> getVariationAssignment(VariationAssignmentRequest request) {
        long startOfRequest = currentTimeMillis();
        // Get experiment
        ExperimentModel expModel = experimentRepository.findById(request.getExperimentId());

        // If experiment doesn't exist, return V0
        if(expModel == null) {
            VariationAssignmentResponse response = VariationAssignmentResponse.builder()
                    .experimentId(request.getExperimentId())
                    .variationAssignment(bucketing(expModel, request.getCustomerId(), request.getSessionId()))
                    .build();
            response.setHasError(true);
            response.setError("Experiment with Id : " + request.getExperimentId() + " doesn't exist");
            long latency = currentTimeMillis() - startOfRequest;
            response.setRequestLatency(latency);

            log.info("experimentService:getVariationAssingment:fail:{}", "Experiment with Id : " + request.getExperimentId() + " doesn't exist" );
            log.info("experimentService:experimentCreation:latency:{}ms", latency);

            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        for (String varId : expModel.getVariations()) {
            ExperimentVariationModel varModel = variationsRepository.findById(varId);
            if (varModel.getVariationWeight() == 100 && varModel.getVariationName().equals(V_0)) {
                VariationAssignmentResponse response = VariationAssignmentResponse.builder()
                        .experimentId(expModel.getId())
                        .variationAssignment(bucketing(expModel, request.getCustomerId(), request.getSessionId()))
                        .build();
                response.setHasError(false);
                response.setError(null);
                long latency = currentTimeMillis() - startOfRequest;
                response.setRequestLatency(latency);
                log.info("experimentService:getVariationAssignment:success");
                log.info("experimentService:experimentCreation:latency:{}ms", latency);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }

        VariationAssignmentResponse response = VariationAssignmentResponse.builder()
                .experimentId(expModel.getId())
                .variationAssignment(bucketing(expModel, request.getCustomerId(), request.getSessionId()))
                .build();
        response.setHasError(false);
        response.setError(null);
        long latency = currentTimeMillis() - startOfRequest;
        response.setRequestLatency(latency);
        log.info("experimentService:getVariationAssignment:success");
        log.info("experimentService:experimentCreation:latency:{}ms", latency);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    private List<ExperimentVariationModel> createVariationsForExperiment(String experimentId, int numberOfVariations) {
        List<String> variationIds = new ArrayList<>();
        List<ExperimentVariationModel> variations = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            ExperimentVariationModel variation = new ExperimentVariationModel();
            variation.setId(UUID.randomUUID().toString());
            variation.setVariationName(VARIATIONS_PREFIX + i);
            variation.setExperimentId(experimentId);
            variation.setVariationWeight(STARTING_WEIGHT);

            variationsRepository.save(variation);
            variationIds.add(variation.getId());
            variations.add(variation);
            log.info("experimentService:createVariationForExperiment:success:{}:{}", experimentId, variation.getId());
        }
        ExperimentModel experimentModel = experimentRepository.findById(experimentId);
        experimentModel.setVariations(variationIds);
        experimentRepository.update(experimentModel);
        return variations;
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

    private HashMap<String, Object> validateModifyVariations(ModifyVariationWeightRequest request) {
        HashMap<String, Object> response = new HashMap<>();
        if (request == null) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Request is Null");
            return response;
        }

        ExperimentModel experimentModel = experimentRepository.findById(request.getExperimentId());
        if (experimentModel == null) {
            response.put("httpStatus", HttpStatus.NOT_FOUND);
            response.put("hasError", true);
            response.put("errorMessage", String.format("Experiment with experiment id: %s, was not found", request.getExperimentId()));
            return response;
        }

        if (request.getExperimentId().isEmpty()) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Request doesn't have an ExperimentId");
            return response;
        }

        if (request.getVariationWeights().isEmpty()) {
            response.put("httpStatus", HttpStatus.BAD_REQUEST);
            response.put("hasError", true);
            response.put("errorMessage", "Request doesn't have variation weights");
            return response;
        }
        double totalWeightOfExperiment = 0;
        for (Double weight : request.getVariationWeights().values()) {
            totalWeightOfExperiment += weight;
            if (totalWeightOfExperiment > 100) {
                response.put("httpStatus", HttpStatus.BAD_REQUEST);
                response.put("hasError", true);
                response.put("errorMessage", "The sum of the weights is bigger than 100.");
                return response;
            }
        }

        response.put("httpStatus", HttpStatus.OK);
        response.put("hasError", false);
        response.put("errorMessage", null);
        return response;
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
