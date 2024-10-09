package com.altair.graphql.util;

import graphql.language.Document;
import graphql.parser.Parser;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GraphQLUtil {

    /**
     * Load and combine GraphQL schemas from the provided schema file path and a gateway schema.
     *
     * @param schemaFilePath Path to the primary schema file
     * @return GraphQLSchema object containing the combined schema
     * @throws IOException if the schema file cannot be read
     */
    public static GraphQLSchema loadSchema(String schemaFilePath) throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        // Read and combine the schemas
        String schema = new String(Files.readAllBytes(Paths.get(schemaFilePath)));
        String gatewaySchema = new String(Files.readAllBytes(Paths.get("src/main/resources/graphql/gateway.graphqls")));
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema + gatewaySchema);

        // Create runtime wiring with custom scalars
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.Json)
                .build();

        // Generate the GraphQL schema
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    /**
     * Validates a GraphQL query against the provided schema.
     *
     * @param schema GraphQL schema used for validation
     * @param type Type of the query (not used currently)
     * @param nodeData Node data to be validated (not used currently)
     * @param locale Locale for validation
     * @param query The GraphQL query string to be validated
     * @return true if the query is valid, false otherwise
     */
    public static boolean validateGraphQL(GraphQLSchema schema, String type, Map<String, Object> nodeData, Locale locale, String query) {
        // Parse the GraphQL query
        Document parsedQuery = new Parser().parseDocument(query);

        // Validate the query
        Validator validator = new Validator();
        List<ValidationError> errors = validator.validateDocument(schema, parsedQuery, locale);

        // Output validation result
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
