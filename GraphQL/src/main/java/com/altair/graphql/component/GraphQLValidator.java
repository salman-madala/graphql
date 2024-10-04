package com.altair.graphql.component;

import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
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
        File schemaFile = new File("src/main/resources/graphql/schema.graphql");
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

    // Recursive validation for nested objects
//    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
//        for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
//            String fieldName = field.getName();
//            GraphQLType fieldType = field.getType();
//
//            // Check if the field is required (non-nullable)
//            boolean isNonNull = fieldType instanceof GraphQLNonNull;
//
//            // If the field is missing
//            if (!nodeData.containsKey(fieldName)) {
//                if (isNonNull) {
//                    return false; // Required field is missing
//                } else {
//                    continue;  // Optional fields can be skipped
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
//            // If the field is present but is not required (nullable), we still check its value
//            if (value == null) {
//                continue; // Optional field can be null
//            }
//
//            // Check if the field is a nested object (e.g., author in Book)
//            if (fieldType instanceof GraphQLObjectType) {
//                if (!(value instanceof Map)) {
//                    return false;  // Invalid nested object format
//                }
//
//                GraphQLObjectType nestedObjectType = (GraphQLObjectType) fieldType;
//                if (!validateFields(nestedObjectType, (Map<String, Object>) value)) {
//                    return false;
//                }
//            }
//        }
//
//        return true;  // All fields are valid
//    }


    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
        for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
            String fieldName = field.getName();
            GraphQLType fieldType = field.getType();

            // Check if the field is required (non-nullable)
            boolean isNonNull = fieldType instanceof GraphQLNonNull;

            System.out.println("Validating field: " + fieldName + " | Required: " + isNonNull);

            // If the field is missing
            if (!nodeData.containsKey(fieldName)) {
                if (isNonNull) {
                    System.out.println("Validation failed: Missing required field '" + fieldName + "'");
                    return false; // Required field is missing
                } else {
                    System.out.println("Optional field '" + fieldName + "' is missing, skipping.");
                    continue;  // Optional fields can be skipped
                }
            }

            Object value = nodeData.get(fieldName);

            // If the field is required (non-nullable) but its value is null or empty, return false
            if (isNonNull && (value == null || value.toString().trim().isEmpty())) {
                System.out.println("Validation failed: Required field '" + fieldName + "' is empty.");
                return false;  // Non-nullable field cannot be empty
            }

            // If the field is present but is not required (nullable), we still check its value
            if (value == null) {
                System.out.println("Optional field '" + fieldName + "' is null, skipping.");
                continue; // Optional field can be null
            }

            // Check if the field is a nested object (e.g., author in Book)
            if (fieldType instanceof GraphQLNonNull) {
                fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
            }
            if (fieldType instanceof GraphQLObjectType) {
                System.out.println("Validating nested object: " + fieldName);
                if (!(value instanceof Map)) {
                    System.out.println("Validation failed: Field '" + fieldName + "' is not a valid object.");
                    return false;  // Invalid nested object format
                }

                GraphQLObjectType nestedObjectType = (GraphQLObjectType) fieldType;
                if (isNonNull && !validateFields(nestedObjectType, (Map<String, Object>) value)) {
                    return false;
                }
            }
        }

        return true;  // All fields are valid
    }

}
