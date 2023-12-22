package com.aovsa.abtestingservice.repositories;

import com.aovsa.abtestingservice.models.ExperimentModel;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

@Component
public class ExperimentRepository {
    private final DynamoDbTemplate dynamoDbTemplate;
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private DynamoDbTable<ExperimentModel> dynamoDbTable;

    public ExperimentRepository( DynamoDbTemplate dynamoDbTemplate,
                                 DynamoDbClient dynamoDbClient,
                                 DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbTemplate = dynamoDbTemplate;
        this.dynamoDbClient = dynamoDbClient;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }
    //TODO: No duplicate experiment names
    public ExperimentModel save(ExperimentModel experimentModel) {
        return dynamoDbTemplate.save(experimentModel);
    }

    public ExperimentModel update(ExperimentModel experimentModel) {
        return dynamoDbTemplate.update(experimentModel);
    }

    public ExperimentModel findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return dynamoDbTemplate.load(key, ExperimentModel.class);
    }

    public List<ExperimentModel> findAll() {
        return dynamoDbTemplate.scanAll(ExperimentModel.class).items().stream().toList();
    }

    public void createTable() {
        dynamoDbTable = dynamoDbEnhancedClient.table("experiment_model", TableSchema.fromBean(ExperimentModel.class));
        dynamoDbTable.createTable();
    }

    public void deleteTable() {
        dynamoDbTable = dynamoDbEnhancedClient.table("experiment_model", TableSchema.fromBean(ExperimentModel.class));
        dynamoDbTable.deleteTable();
    }
}
