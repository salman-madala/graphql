package com.altair.graphql.component;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.*;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.GraphCreateOptions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.json.JSONObject;

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
            return (T) createGraph(environment);
        } if (fieldName.contains("create")) {
            return (T) create(environment);
        } else {
            throw new UnsupportedOperationException("Unknown mutation: " + fieldName);
        }
    }

    private T create(DataFetchingEnvironment environment) {
        String fieldName = environment.getFieldDefinition().getName();
        try {
            String type = environment.getArgument("type");
            if (fieldName.toLowerCase().contains("node")) {
                BaseDocument node = new BaseDocument();
                Map<String, Object> nodeData = environment.getArgument("nodeData");
                if (!arangoDatabase.collection(type).exists()) {
                    arangoDatabase.createCollection(type);
                }
                node.addAttribute("nodeData", nodeData);
                Boolean isValid = graphQLValidator.validateNodeData(type, nodeData);
                System.out.println(isValid);

                if(isValid) {
                    DocumentCreateEntity response = arangoDatabase.collection(type).insertDocument(node);

                    Map<String, Object> result = new HashMap<>();
                    result.put("_id", response.getKey());
                    result.put("type", type);
                    result.put("nodeData", nodeData);

                    return (T) result;
                }else{
                    throw new IllegalArgumentException("Invalid nodeData for type: " + type);
                }
            } else {
                BaseDocument edge = new BaseDocument();
                String from = environment.getArgument("from");
                String to = environment.getArgument("to");
                String label = environment.getArgument("label");
                edge.addAttribute("_from", from);
                edge.addAttribute("_to", to);
                edge.addAttribute("label", label);
                if (!arangoDatabase.collection(label+"_edges").exists()) {
                    arangoDatabase.createCollection(label+"_edges",new CollectionCreateOptions().type(CollectionType.EDGES));
                }
                DocumentCreateEntity response = arangoDatabase.collection(label+"_edges").insertDocument(edge);
                Map<String, Object> result = new HashMap<>();
                result.put("_id", response.getId());
                result.put("label", label);
                result.put("_from", from);
                result.put("_to", to);
                return (T) result;
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    private T createGraph(DataFetchingEnvironment environment) {
        try {
            String graphName = environment.getArgument("graphName");
            String edgeDefinitioncolection = environment.getArgument("edgeDefinition");
            String fromCollectionsStr = environment.getArgument("fromCollections");
            String toCollectionsStr = environment.getArgument("toCollections");

            String[] fromCollections = fromCollectionsStr.split(",");
            String[] toCollections = toCollectionsStr.split(",");

            EdgeDefinition edgeDefinition = new EdgeDefinition()
                    .collection(edgeDefinitioncolection)    // Edge collection
                    .from(fromCollections) // From collection(s)
                    .to(toCollections);

            GraphEntity graph = arangoDatabase.createGraph(graphName, Collections.singletonList(edgeDefinition), new GraphCreateOptions());
            System.out.println("Graph " + graph.getName() + " created successfully.");
            return (T) ("Graph " + graph.getName() + " created successfully.");
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}


