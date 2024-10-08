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
        String gatewaySchema = new String(Files.readAllBytes(Paths.get("src/main/resources/graphql/gateway.graphql")));

        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema+gatewaySchema);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring =  RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json).build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        return graphQLSchema;
    }

    public static boolean validateGraphQL(GraphQLSchema schema, String type,Map<String, Object> nodeData,Locale locale,String query) {

//        String test = "mutation {\n" +
//                "          createNode(input: {\n" +
//                "            nodeData: {\n" +
//                "              name: \"ReactJS\"\n" +
//                "              description: 9703395735\n" +
//                "              price: 5000 \n" +
//                "            }\n" +
//                "            type: 3000,\n" +
//                "            schema_file: \"D:/GraphQL/java/graph/GraphQL/src/main/resources/graphql/book.graphql\"\n" +
//                "          }) {\n" +
//                "            _id\n" +
//                "            type\n" +
//                "            nodeData\n" +
//                "          }\n" +
//                "        }";

        Document parsedQuery = new Parser().parseDocument(query);

        // Validate the parsed query against the schema
        Validator validator = new Validator();
        List<ValidationError> errors = validator.validateDocument(schema, parsedQuery,locale);

        // Output validation results
        if (errors.isEmpty()) {
            System.out.println("Query is valid.");
            return true;
        } else {
            System.out.println("Validation errors:");
            errors.forEach(System.out::println);
            return false;
        }













//        Document parsedQuery = new Parser().parseDocument(query);
//
//        Validator validator = new Validator();
//        List<ValidationError> errors = validator.validateDocument(schema, parsedQuery,locale);
//
//        if (errors.isEmpty()) {
//            System.out.println("Query is valid.");
//            return true;
//        } else {
//            StringBuilder errorMessage = new StringBuilder("Validation errors:");
//            errors.forEach(error -> errorMessage.append("\n").append(error.getMessage()));
//            throw new IllegalArgumentException(errorMessage.toString());
//        }




//        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
//        ExecutionResult result = graphQL.execute(query);
//        if (!result.getErrors().isEmpty()) {
//            throw new RuntimeException("Validation failed: " + result.getErrors().toString());
//        }
//        return true;





    }

}
