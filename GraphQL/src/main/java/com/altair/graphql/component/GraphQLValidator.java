package com.altair.graphql.component;

import graphql.language.Document;
import graphql.parser.Parser;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorCollector;
import graphql.validation.Validator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GraphQLValidator {

    public boolean validateNodeData(String schemaFilePath,String query) {

        SchemaParser schemaParser = new SchemaParser();
        File schemaFile = new File(schemaFilePath);
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaFile);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json).build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

        String mutationQuery = """
        mutation MyMutation2 {
          createNode(input: {
            nodeData: {
              name: "Subbu"
              email: "subbu@gmail.com"
              phoneNumber: 9703395735
              address: { city: "kurnool", pinCode: 522213 }
            }
            type: "User",
            schema_file: "D:/salman/GraphQL/Latest/graphql/GraphQL/src/main/resources/graphql/user.graphql"
          }) {
            _id
            type
            nodeData
          }
        }
        """;


        Document parsedQuery = new Parser().parseDocument(mutationQuery);

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, parsedQuery, Locale.getDefault());


        // Output validation results
        if (validationErrors .isEmpty()) {
            System.out.println("Query is valid.");
            return true;
        } else {
            System.out.println("Validation errors:");
            validationErrors .forEach(System.out::println);
            return false;
        }











//        if (graphQLType instanceof GraphQLObjectType) {
//            GraphQLObjectType objectType = (GraphQLObjectType) graphQLType;
//            return validateFields(objectType, nodeData);
//        }
//        return false;
    }


//    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
//        for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
//            String fieldName = field.getName();
//            GraphQLType fieldType = field.getType();
//
//            // Check if the field is required (non-nullable)
//            boolean isNonNull = fieldType instanceof GraphQLNonNull;
//
//
//            // If the field is missing
//            if (!nodeData.containsKey(fieldName)) {
//                if (isNonNull) {
//                    return false;
//                } else {
//                    continue;
//                }
//            }
//
//            Object value = nodeData.get(fieldName);
//
//            // If the field is required (non-nullable) but its value is null or empty, return false
//            if (isNonNull && (value == null || value.toString().trim().isEmpty())) {
//                return false;  // Non-nullable field cannot be empty
//            }
//
//
//            if (value == null) {
//                continue;
//            }
//
//            // Check if the field is a nested object (e.g., author in Book)
//            if (fieldType instanceof GraphQLNonNull) {
//                fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
//            }
//            if (fieldType instanceof GraphQLObjectType) {
//                if (!(value instanceof Map)) {
//                    return false;
//                }
//
//                GraphQLObjectType nestedObjectType = (GraphQLObjectType) fieldType;
//                if (isNonNull && !validateFields(nestedObjectType, (Map<String, Object>) value)) {
//                    return false;
//                }
//            }
//        }
//
//        return true;
//    }

}
