package com.altair.graphql.component;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class GraphQLValidator {

    public boolean validateNodeData(String type, Map<String, Object> nodeData) {

        SchemaParser schemaParser = new SchemaParser();
        File schemaFile = new File("src/main/resources/graphql/schema.graphqls");
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaFile);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json).build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);


        GraphQLType graphQLType = graphQLSchema.getType(type);

        if (graphQLType instanceof GraphQLObjectType) {
            GraphQLObjectType objectType = (GraphQLObjectType) graphQLType;
            return validateFields(objectType, nodeData);
        }
        return false;
    }

    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
        return objectType.getFieldDefinitions().stream()
                .allMatch(field -> nodeData.containsKey(field.getName()));
    }
}
