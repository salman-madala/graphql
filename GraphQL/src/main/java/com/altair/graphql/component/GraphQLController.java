package com.altair.graphql.component;

import com.altair.graphql.config.GraphQLSchemaBuilder;
import graphql.GraphQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/graphql")
public class GraphQLController {

    @Autowired
    private GraphQLSchemaBuilder graphQLSchemaConfig;


    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, String> request) {
        try {
            String query = (String) request.get("query");
            GraphQL graphQL = graphQLSchemaConfig.graphQL();
            return graphQL.execute(query).getData();
        }catch (Exception e){
            System.out.println(e);
            return (Map<String, Object>) e;
        }
    }


}
