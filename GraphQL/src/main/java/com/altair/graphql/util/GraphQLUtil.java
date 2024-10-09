package com.altair.graphql.util;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.Document;
import graphql.language.ObjectTypeDefinition;
import graphql.parser.Parser;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import graphql.validation.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GraphQLUtil {

    public static GraphQLSchema loadSchema(String schemaFilePath) throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        String schema = new String(Files.readAllBytes(Paths.get(schemaFilePath)));
        String gatewaySchema = new String(Files.readAllBytes(Paths.get("src/main/resources/graphql/gateway.graphqls")));

        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema+gatewaySchema);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring =  RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json).build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        return graphQLSchema;
    }

    public static boolean validateGraphQL(GraphQLSchema schema, String type,Map<String, Object> nodeData,Locale locale,String query) {
        Document parsedQuery = new Parser().parseDocument(query);
        Validator validator = new Validator();
        List<ValidationError> errors = validator.validateDocument(schema, parsedQuery,locale);

        if (errors.isEmpty()) {
            System.out.println("Query is valid.");
            return true;
        } else {
            System.out.println("Validation errors:");
            errors.forEach(System.out::println);
            return false;
        }

    }

}