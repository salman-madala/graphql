package com.altair.graphql.service;

import com.altair.graphql.config.GraphQLSchemaBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GraphQLService {

    @Autowired
    private GraphQLSchemaBuilder graphQLSchemaConfig;

    public Map<String, Object> executeQuery(String query) throws Exception {
        GraphQL graphQL = graphQLSchemaConfig.graphQL(query);
        ExecutionResult executionResult = graphQL.execute(query);
        if (executionResult.getErrors().isEmpty()) {
            return executionResult.getData();
        } else {
            throw new RuntimeException("Query execution errors: " + executionResult.getErrors());
        }
    }

}
