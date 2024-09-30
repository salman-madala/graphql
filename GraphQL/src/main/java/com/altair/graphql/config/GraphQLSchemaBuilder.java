package com.altair.graphql.config;

import com.altair.graphql.component.ArangoDataFetcher;
import com.altair.graphql.component.ArangoDataMutation;
import com.arangodb.ArangoDatabase;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.template.ArangoTemplate;
import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeName;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class GraphQLSchemaBuilder {

    private ArangoDbConfig arangoConfig = new ArangoDbConfig();
    private final ArangoDatabase arangoDatabase;
    private final ArangoTemplate arangoTemplate;
    @Autowired
    private ArangoOperations arangoOperations;

    public GraphQLSchemaBuilder() throws Exception {
        this.arangoDatabase = arangoConfig.arangoDatabase();
        this.arangoTemplate = (ArangoTemplate) arangoConfig.arangoTemplate();
    }

    public TypeDefinitionRegistry typeDefinitionRegistry() throws ClassNotFoundException {
        SchemaParser schemaParser = new SchemaParser();
        File schemaFile = new File("src/main/resources/graphql/schema.graphqls");
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaFile);
        return typeRegistry;
    }
    public GraphQL graphQL() throws ClassNotFoundException {
        TypeDefinitionRegistry typeRegistry = typeDefinitionRegistry();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = buildRuntimeWiring(typeRegistry);
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    private RuntimeWiring buildRuntimeWiring(TypeDefinitionRegistry typeRegistry) {

        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Json);


        // Loop through all types in the TypeDefinitionRegistry
        typeRegistry.types().forEach((typeName, typeDefinition) -> {
            if (typeDefinition instanceof ObjectTypeDefinition) {
                ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) typeDefinition;
                if (typeDefinition.getName().equals("Query") ) {
                    runtimeWiringBuilder.type(typeName, typeWiring -> {
                        // For each field in the object type, create a GenericDataFetcher if no specific fetcher is provided
                        objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                            String fieldName = fieldDefinition.getName();
                            String collectionName = extractCollectionName(fieldDefinition);
                            DataFetcher<?> dataFetcher = new ArangoDataFetcher(arangoDatabase, collectionName,arangoOperations);
                            typeWiring.dataFetcher(fieldName, dataFetcher);
                        });
                        return typeWiring;
                    });
                } else if (typeDefinition.getName().equals("Mutation")) {
                    runtimeWiringBuilder.type(typeName, typeWiring -> {
                        // For each field in the object type, create a GenericDataFetcher if no specific fetcher is provided
                        objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                            String fieldName = fieldDefinition.getName();
                            String collectionName = extractCollectionName(fieldDefinition);

                            // Automatically bind an ArangoDB data fetcher for each field
                            DataFetcher<?> dataFetcher = new ArangoDataMutation<>(arangoDatabase, collectionName);
//                            DataFetcher<?> dataFetcher = new GenericArangoDataFetcher(arangoTemplate,typeClass);
                            typeWiring.dataFetcher(fieldName, dataFetcher);
                        });
                        return typeWiring;
                    });
                }
            }
        });

        return runtimeWiringBuilder.build();

    }

    // Helper method to extract collection name
    private String extractCollectionName(FieldDefinition fieldDefinition) {
        if (fieldDefinition.getType() instanceof ListType) {
            ListType fieldType = (ListType) fieldDefinition.getType();
            TypeName type = (TypeName) fieldType.getType();
            return type.getName().toLowerCase();
        } else {
            TypeName type = (TypeName) fieldDefinition.getType();
            return type.getName().toLowerCase();
        }
    }

}
