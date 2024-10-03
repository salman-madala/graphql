package com.altair.graphql.component;

import com.altair.graphql.config.GraphQLSchemaBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/graphql")
public class GraphQLController {

    @Autowired
    private GraphQLSchemaBuilder graphQLSchemaConfig;


    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            GraphQL graphQL = graphQLSchemaConfig.graphQL();
            ExecutionResult executionResult = graphQL.execute(query);
            Map<String, Object> response = executionResult.getData();
            if (executionResult.getErrors().isEmpty()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


}
