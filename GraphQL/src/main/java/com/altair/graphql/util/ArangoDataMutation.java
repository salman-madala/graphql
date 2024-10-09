package com.altair.graphql.util;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.*;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.GraphCreateOptions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArangoDataMutation<T> implements DataFetcher<T> {

    private final ArangoDatabase arangoDatabase;
    private final String collectionName;
    private final GraphQLValidator graphQLValidator;
    private final String query;

    public ArangoDataMutation(ArangoDatabase arangoDatabase, String collectionName, GraphQLValidator graphQLValidator, String query) {
        this.arangoDatabase = arangoDatabase;
        this.collectionName = collectionName;
        this.graphQLValidator = graphQLValidator;
        this.query = query;
    }

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {
        String fieldName = environment.getFieldDefinition().getName();

        if (fieldName.contains("createGraph")) {
            return createGraph(environment);
        } else if (fieldName.contains("create")) {
            return create(environment);
        } else {
            throw new UnsupportedOperationException("Unknown mutation: " + fieldName);
        }
    }

    // Handles the create node or edge mutation
    private T create(DataFetchingEnvironment environment) {
        String fieldName = environment.getFieldDefinition().getName();

        try {
            if (isNodeMutation(fieldName)) {
                return createNode(environment);
            } else {
                return createEdge(environment);
            }
        } catch (Exception e) {
            handleException(e);
        }

        return null;
    }

    // Handles the createGraph mutation
    private T createGraph(DataFetchingEnvironment environment) {
        try {
            String graphName = environment.getArgument("graphName");
            String edgeDefinitionCollection = environment.getArgument("edgeDefinition");
            String fromCollectionsStr = environment.getArgument("fromCollections");
            String toCollectionsStr = environment.getArgument("toCollections");

            return createGraphInArangoDB(graphName, edgeDefinitionCollection, fromCollectionsStr, toCollectionsStr);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    // Private helper methods for handling specific tasks
    private boolean isNodeMutation(String fieldName) {
        return fieldName.toLowerCase().contains("node");
    }

    private T createNode(DataFetchingEnvironment environment) throws Exception {
        Map<String, Object> input = environment.getArgument("input");
        String type = (String) input.get("type");
        Map<String, Object> nodeData = (Map<String, Object>) input.get("nodeData");
        String schemaFilePath = (String) input.get("schema_file");

        // Load and validate schema
        GraphQLSchema schema = GraphQLUtil.loadSchema(schemaFilePath);
        boolean isQueryValid = GraphQLUtil.validateGraphQL(schema, type, nodeData, environment.getLocale(), query);

        if (!isQueryValid) {
            throw new IllegalArgumentException("Invalid Query");
        }

        // Validate JSON data
        if (!graphQLValidator.validateNodeData(schema, type, nodeData)) {
            throw new IllegalArgumentException("Invalid nodeData for type: " + type);
        }

        // Insert node into ArangoDB
        if (!arangoDatabase.collection(type).exists()) {
            arangoDatabase.createCollection(type);
        }

        return insertNodeIntoCollection(type, nodeData);
    }

    private T insertNodeIntoCollection(String type, Map<String, Object> nodeData) {
        BaseDocument node = new BaseDocument();
        node.addAttribute("nodeData", nodeData);
        DocumentCreateEntity response = arangoDatabase.collection(type).insertDocument(node);

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("_id", response.getKey());
        result.put("type", type);
        result.put("nodeData", nodeData);

        return (T) result;
    }

    private T createEdge(DataFetchingEnvironment environment) {
        String from = environment.getArgument("from");
        String to = environment.getArgument("to");
        String label = environment.getArgument("label");

        if (!arangoDatabase.collection(label + "_edges").exists()) {
            arangoDatabase.createCollection(label + "_edges", new CollectionCreateOptions().type(CollectionType.EDGES));
        }

        return insertEdgeIntoCollection(label, from, to);
    }

    private T insertEdgeIntoCollection(String label, String from, String to) {
        BaseDocument edge = new BaseDocument();
        edge.addAttribute("_from", from);
        edge.addAttribute("_to", to);
        edge.addAttribute("label", label);

        DocumentCreateEntity response = arangoDatabase.collection(label + "_edges").insertDocument(edge);

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("_id", response.getId());
        result.put("label", label);
        result.put("_from", from);
        result.put("_to", to);

        return (T) result;
    }

    private T createGraphInArangoDB(String graphName, String edgeDefinitionCollection, String fromCollectionsStr, String toCollectionsStr) {
        String[] fromCollections = fromCollectionsStr.split(",");
        String[] toCollections = toCollectionsStr.split(",");

        EdgeDefinition edgeDefinition = new EdgeDefinition()
                .collection(edgeDefinitionCollection)
                .from(fromCollections)
                .to(toCollections);

        GraphEntity graph = arangoDatabase.createGraph(graphName, Collections.singletonList(edgeDefinition), new GraphCreateOptions());
        System.out.println("Graph " + graph.getName() + " created successfully.");

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Graph " + graph.getName() + " is created successfully.");

        return (T) result;
    }

    // Handle exceptions in a unified way
    private void handleException(Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
    }
}
