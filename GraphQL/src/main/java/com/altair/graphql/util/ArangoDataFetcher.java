package com.altair.graphql.util;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.arangodb.springframework.core.ArangoOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        if (fieldName.equals("traverseGraph")) {
            return (T) traverseGraph(environment);
        }else if (fieldName.equals("search")) {
            return (T) search(environment);
        }else {
            String documentKey = environment.getArgument("id");
            String type = environment.getArgument("type");
            if (documentKey != null) {
                String query = "FOR doc IN " + type + " FILTER doc._key == @key RETURN doc";
                Map<String, Object> bindVars = new HashMap<>();
                bindVars.put("key", documentKey);
                try (ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null)) {
                    Map<String, Object> res = cursor.hasNext() ? cursor.next() : null;
                    return (T) res;
                }
            } else {
                String query = "FOR doc IN " + type + " RETURN doc";
                try (ArangoCursor<BaseDocument> cursor = arangoDatabase.query(query, null, null, null)) {
                    Collection<BaseDocument> allDocuments = cursor.asListRemaining();

                    System.out.println("Fetched all documents: " + allDocuments);
                    return (T) allDocuments;
                }
            }
        }
    }

    private T traverseGraph(DataFetchingEnvironment environment) {
        try {
            String startId = environment.getArgument("startId");
            Integer depth = Integer.parseInt(environment.getArgument("depth"));
            String graphName = environment.getArgument("graphName");
            String direction = environment.getArgument("direction");
            Map<String, Object> bindVars = new HashMap<>();
            String query = "FOR vertex, edge, path IN 1.." + depth + " " + direction + "  @startId GRAPH @graphName RETURN { vertex: vertex, edge: edge, path: path }";
            bindVars.put("startId", startId);
            bindVars.put("graphName", graphName);

            ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null);
            List<Map<String, Object>> results = new ArrayList<>();
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
            return (T) results;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private T search(DataFetchingEnvironment environment) {
        try {
            String analyzer = environment.getArgument("analyzer");
            String collectionName = environment.getArgument("collectionName");
            String searchVal = environment.getArgument("searchVal");
            String searchProperty = environment.getArgument("searchProperty");
            Integer offset = environment.getArgument("offset");
            Integer count = environment.getArgument("count");
            String operator = environment.getArgument("operator");
            String query = "";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@collectionName", collectionName);
            bindVars.put("param0", searchVal);
            bindVars.put("attr0", searchProperty.split("\\."));
            if(analyzer == null){
                if(searchProperty.equals("__fullText")){
                    searchVal = "%"+searchVal.toLowerCase()+"%";
                }

                bindVars.put("offset", offset);
                bindVars.put("count", count);
                query = "FOR x IN @@collectionName LET att = APPEND(SLICE(ATTRIBUTES(x), 0, 25), \"_key\", true) FILTER x.@attr0 "+operator+" @param0 LIMIT @offset, @count RETURN KEEP(x, att)";
                if(operator.toLowerCase().contains("ilike")){
                    query = "FOR x IN @@collectionName  LET att = APPEND(SLICE(ATTRIBUTES(x), 0, 25), \"_key\", true)  FILTER LOWER(x.@attr0) LIKE LOWER(@param0) LIMIT @offset, @count RETURN KEEP(x, att)";
                }
            }else{
                bindVars.put("analyzer", analyzer);
                query = "FOR x IN @@collectionName SEARCH ANALYZER(PHRASE(x.@attr0, LOWER(@param0)), @analyzer) RETURN x";
            }

            ArangoCursor<Map> cursor = arangoDatabase.query(query, Map.class, bindVars, null);
            List<Map<String, Object>> results = new ArrayList<>();
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("allResults",results);
            return (T) result;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }


}
