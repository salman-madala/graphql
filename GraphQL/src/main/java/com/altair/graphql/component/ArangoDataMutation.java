package com.altair.graphql.component;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.*;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.springframework.core.ArangoOperations;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArangoDataMutation<T> implements DataFetcher<T> {

    private final ArangoDatabase arangoDatabase;
    private final String collectionName;

    public ArangoDataMutation(ArangoDatabase arangoDatabase, String collectionName) {
        this.arangoDatabase = arangoDatabase;
        this.collectionName = collectionName;
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

            String id = environment.getArgument("id");
            String type = environment.getArgument("type");
            if (fieldName.toLowerCase().contains("node")) {
                BaseDocument node = new BaseDocument();
                Map<String, Object> nodeData = environment.getArgument("nodeData");
                if (!arangoDatabase.collection(type).exists()) {
                    arangoDatabase.createCollection(type);
                }
                node.addAttribute("nodeData", nodeData);
                DocumentCreateEntity response = arangoDatabase.collection(type).insertDocument(node);

                Map<String, Object> result = new HashMap<>();
                result.put("_id", response.getKey());
                result.put("type", type);
                result.put("nodeData", nodeData);

                return (T) result;
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


