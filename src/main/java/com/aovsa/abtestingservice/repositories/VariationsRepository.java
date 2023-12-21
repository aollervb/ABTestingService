package com.aovsa.abtestingservice.repositories;

import com.aovsa.abtestingservice.models.ExperimentModel;
import com.aovsa.abtestingservice.models.ExperimentVariationModel;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

@Component
public class VariationsRepository {
    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private DynamoDbTable<ExperimentModel> dynamoDbTable;

    public VariationsRepository( DynamoDbTemplate dynamoDbTemplate,
                                 DynamoDbClient dynamoDbClient,
                                 DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbTemplate = dynamoDbTemplate;
        this.dynamoDbClient = dynamoDbClient;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    public ExperimentVariationModel save(ExperimentVariationModel variationModel) {
        return dynamoDbTemplate.save(variationModel);
    }

    public ExperimentVariationModel update(ExperimentVariationModel variationModel) {
        return dynamoDbTemplate.update(variationModel);
    }

    public ExperimentVariationModel findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return dynamoDbTemplate.load(key, ExperimentVariationModel.class);
    }

    public List<ExperimentVariationModel> findAll() {
        return dynamoDbTemplate.scanAll(ExperimentVariationModel.class).items().stream().toList();
    }

    public void createTable() {
        dynamoDbTable = dynamoDbEnhancedClient.table("experiment_variation_model", TableSchema.fromBean(ExperimentModel.class));
        dynamoDbTable.createTable();
    }

    public void deleteTable() {
        dynamoDbTable = dynamoDbEnhancedClient.table("experiment_variation_model", TableSchema.fromBean(ExperimentModel.class));
        dynamoDbTable.deleteTable();
    }
}
