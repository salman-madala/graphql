package com.altair.graphql.util;

import graphql.schema.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GraphQLValidator {

    // Public method to validate node data against the schema type
    public boolean validateNodeData(GraphQLSchema graphQLSchema, String type, Map<String, Object> nodeData) {
        GraphQLType graphQLType = graphQLSchema.getType(type);
        if (graphQLType instanceof GraphQLObjectType) {
            GraphQLObjectType objectType = (GraphQLObjectType) graphQLType;
            return validateFields(objectType, nodeData);
        }
        return false;
    }

    // Private method to validate fields of a GraphQL object type
    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
        for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
            String fieldName = field.getName();
            GraphQLType fieldType = field.getType();
            boolean isNonNull = fieldType instanceof GraphQLNonNull;

            // Check if the field is present or optional
            if (!nodeData.containsKey(fieldName)) {
                if (isNonNull) {
                    return false; // Required field is missing
                } else {
                    continue; // Optional field, skip validation
                }
            }

            Object value = nodeData.get(fieldName);

            // Validate non-nullable fields
            if (isNonNull && (value == null || value.toString().trim().isEmpty())) {
                return false; // Non-nullable field cannot be null or empty
            }

            // Skip null values for optional fields
            if (value == null) {
                continue;
            }

            // If the field is non-null, unwrap the underlying type
            if (fieldType instanceof GraphQLNonNull) {
                fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
            }

            // If the field is an object type, recursively validate its fields
            if (fieldType instanceof GraphQLObjectType) {
                if (!(value instanceof Map)) {
                    return false; // Value should be a map for object types
                }
                GraphQLObjectType nestedObjectType = (GraphQLObjectType) fieldType;
                if (!validateFields(nestedObjectType, (Map<String, Object>) value)) {
                    return false; // Recursive validation failed
                }
            }
        }
        return true; // All fields passed validation
    }
}
