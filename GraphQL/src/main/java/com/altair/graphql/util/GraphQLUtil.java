package com.altair.graphql.util;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GraphQLUtil {
    public static GraphQLSchema loadSchema(String schemaFilePath) throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        File schemaFile = new File(schemaFilePath);
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaFile);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json).build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        return graphQLSchema;
    }
}
