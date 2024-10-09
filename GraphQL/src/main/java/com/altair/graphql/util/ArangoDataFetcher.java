package com.altair.graphql.util;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.arangodb.springframework.core.ArangoOperations;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.io.IOException;
import java.util.*;

public class ArangoDataFetcher<T> implements DataFetcher<T> {

    private final ArangoDatabase arangoDatabase;
    private final String collectionName;
    private final ArangoOperations arangoOperations;

    public ArangoDataFetcher(ArangoDatabase arangoDatabase, String collectionName, ArangoOperations arangoOperations) {
        this.arangoDatabase = arangoDatabase;
        this.collectionName = collectionName;
        this.arangoOperations = arangoOperations;
    }

    @Override
    public T get(DataFetchingEnvironment environment) throws IOException {
        String fieldName = environment.getFieldDefinition().getName();

        switch (fieldName) {
            case "traverseGraph":
                return (T) traverseGraph(environment);
            case "search":
                return (T) search(environment);
            default:
                return (T) fetchDocument(environment);
        }
    }

    // Fetch a single document or all documents from a collection
    private T fetchDocument(DataFetchingEnvironment environment) throws IOException {
        String documentKey = environment.getArgument("id");
        String type = environment.getArgument("type");

        if (documentKey != null) {
            return (T) fetchDocumentById(documentKey, type);
        } else {
            return (T) fetchAllDocuments(type);
        }
    }

    // Fetch document by ID
    private Map<String, Object> fetchDocumentById(String documentKey, String type) throws IOException {
        String query = "FOR doc IN " + type + " FILTER doc._key == @key RETURN doc";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("key", documentKey);

        try (ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null)) {
            return cursor.hasNext() ? cursor.next() : null;
        }
    }

    // Fetch all documents from a collection
    private Collection<BaseDocument> fetchAllDocuments(String type) {
        String query = "FOR doc IN " + type + " RETURN doc";

        try (ArangoCursor<BaseDocument> cursor = arangoDatabase.query(query, null, null, null)) {
            return cursor.asListRemaining();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Traverse a graph in ArangoDB
    private T traverseGraph(DataFetchingEnvironment environment) {
        try {
            String startId = environment.getArgument("startId");
            Integer depth = Integer.parseInt(environment.getArgument("depth"));
            String graphName = environment.getArgument("graphName");
            String direction = environment.getArgument("direction");

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("startId", startId);
            bindVars.put("graphName", graphName);

            String query = String.format(
                    "FOR vertex, edge, path IN 1..%d %s @startId GRAPH @graphName RETURN { vertex: vertex, edge: edge, path: path }",
                    depth, direction
            );

            ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null);
            List<Map<String, Object>> results = new ArrayList<>();

            while (cursor.hasNext()) {
                results.add(cursor.next());
            }

            return (T) results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Search using the given parameters
    private T search(DataFetchingEnvironment environment) {
        try {
            String analyzer = environment.getArgument("analyzer");
            String collectionName = environment.getArgument("collectionName");
            String searchVal = environment.getArgument("searchVal");
            String searchProperty = environment.getArgument("searchProperty");
            Integer offset = environment.getArgument("offset");
            Integer count = environment.getArgument("count");
            String operator = environment.getArgument("operator");

            String query = buildSearchQuery(analyzer, searchProperty, operator);
            Map<String, Object> bindVars = buildSearchBindVars(analyzer, collectionName, searchVal, searchProperty, offset, count);

            ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null);
            List<Map<String, Object>> results = new ArrayList<>();

            while (cursor.hasNext()) {
                results.add(cursor.next());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("allResults", results);
            return (T) result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Build the search query based on the parameters
    private String buildSearchQuery(String analyzer, String searchProperty, String operator) {
        if (analyzer == null) {
            if (searchProperty.equals("__fullText")) {
                return "FOR x IN @@collectionName LET att = APPEND(SLICE(ATTRIBUTES(x), 0, 25), \"_key\", true) FILTER LOWER(x.@attr0) LIKE LOWER(@param0) LIMIT @offset, @count RETURN KEEP(x, att)";
            }
            return "FOR x IN @@collectionName LET att = APPEND(SLICE(ATTRIBUTES(x), 0, 25), \"_key\", true) FILTER x.@attr0 " + operator + " @param0 LIMIT @offset, @count RETURN KEEP(x, att)";
        } else {
            return "FOR x IN @@collectionName SEARCH ANALYZER(PHRASE(x.@attr0, LOWER(@param0)), @analyzer) RETURN x";
        }
    }

    // Build the search bind variables map
    private Map<String, Object> buildSearchBindVars(String analyzer, String collectionName, String searchVal, String searchProperty, Integer offset, Integer count) {
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@collectionName", collectionName);
        bindVars.put("param0", searchVal);
        bindVars.put("attr0", searchProperty.split("\\."));

        if (analyzer == null) {
            if (searchProperty.equals("__fullText")) {
                searchVal = "%" + searchVal.toLowerCase() + "%";
            }
            bindVars.put("offset", offset);
            bindVars.put("count", count);
        } else {
            bindVars.put("analyzer", analyzer);
        }

        return bindVars;
    }
}
