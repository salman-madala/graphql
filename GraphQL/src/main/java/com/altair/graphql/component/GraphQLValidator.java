package com.altair.graphql.component;

import graphql.GraphQL;
import graphql.schema.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GraphQLValidator {

    public boolean validateNodeData(GraphQLSchema graphQLSchema, String type, Map<String, Object> nodeData) {
        GraphQLType graphQLType = graphQLSchema.getType(type);
        if (graphQLType instanceof GraphQLObjectType) {
            GraphQLObjectType objectType = (GraphQLObjectType) graphQLType;
            return validateFields(objectType, nodeData);
        }
        return false;
    }

    private boolean validateFields(GraphQLObjectType objectType, Map<String, Object> nodeData) {
        for (GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
            String fieldName = field.getName();
            GraphQLType fieldType = field.getType();
            boolean isNonNull = fieldType instanceof GraphQLNonNull;
            if (!nodeData.containsKey(fieldName)) {
                if (isNonNull) {
                    return false;
                } else {
                    continue;
                }
            }
            Object value = nodeData.get(fieldName);
            if (isNonNull && (value == null || value.toString().trim().isEmpty())) {
                return false;  // Non-nullable field cannot be empty
            }
            if (value == null) {
                continue;
            }
            // Check if the field is a nested object (e.g., author in Book)
            if (fieldType instanceof GraphQLNonNull) {
                fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
            }
            if (fieldType instanceof GraphQLObjectType) {
                if (!(value instanceof Map)) {
                    return false;
                }
                GraphQLObjectType nestedObjectType = (GraphQLObjectType) fieldType;
                if (isNonNull && !validateFields(nestedObjectType, (Map<String, Object>) value)) {
                    return false;
                }
            }
        }
        return true;
    }

}
