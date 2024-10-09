package com.altair.graphql.config;

import com.altair.graphql.util.ArangoDataFetcher;
import com.altair.graphql.util.ArangoDataMutation;
import com.altair.graphql.util.GraphQLValidator;
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
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class GraphQLSchemaBuilder {

    private ArangoDbConfig arangoConfig = new ArangoDbConfig();
    private final ArangoDatabase arangoDatabase;
    private final ArangoTemplate arangoTemplate;
    @Autowired
    private ArangoOperations arangoOperations;
    @Autowired
    @Lazy
    private GraphQLValidator graphQLValidator;

    public GraphQLSchemaBuilder() throws Exception {
        this.arangoDatabase = arangoConfig.arangoDatabase();
        this.arangoTemplate = (ArangoTemplate) arangoConfig.arangoTemplate();
    }

    public GraphQL graphQL(String query) throws Exception {
        TypeDefinitionRegistry typeRegistry = typeDefinitionRegistry();
        RuntimeWiring runtimeWiring = buildRuntimeWiring(typeRegistry,query);
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    private TypeDefinitionRegistry typeDefinitionRegistry() throws Exception {
        SchemaParser schemaParser = new SchemaParser();
        File schemaFile = new File("src/main/resources/graphql/gateway.graphqls");
        return schemaParser.parse(schemaFile);
    }

    private RuntimeWiring buildRuntimeWiring(TypeDefinitionRegistry typeRegistry,String query) {
        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.Json);

        typeRegistry.types().forEach((typeName, typeDefinition) -> {
            if (typeDefinition instanceof ObjectTypeDefinition) {
                ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) typeDefinition;
                if ("Query".equals(typeDefinition.getName())) {
                    buildQueryWiring(runtimeWiringBuilder, objectTypeDefinition);
                } else if ("Mutation".equals(typeDefinition.getName())) {
                    buildMutationWiring(runtimeWiringBuilder, objectTypeDefinition,query);
                }
            }
        });

        return runtimeWiringBuilder.build();
    }

    private void buildQueryWiring(RuntimeWiring.Builder runtimeWiringBuilder, ObjectTypeDefinition objectTypeDefinition) {
        runtimeWiringBuilder.type(objectTypeDefinition.getName(), typeWiring -> {
            objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                DataFetcher<?> dataFetcher = new ArangoDataFetcher<>(arangoDatabase, extractCollectionName(fieldDefinition), arangoOperations);
                typeWiring.dataFetcher(fieldDefinition.getName(), dataFetcher);
            });
            return typeWiring;
        });
    }

    private void buildMutationWiring(RuntimeWiring.Builder runtimeWiringBuilder, ObjectTypeDefinition objectTypeDefinition,String query) {
        runtimeWiringBuilder.type(objectTypeDefinition.getName(), typeWiring -> {
            objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                DataFetcher<?> dataFetcher = new ArangoDataMutation<>(arangoDatabase, extractCollectionName(fieldDefinition), graphQLValidator,query);
                typeWiring.dataFetcher(fieldDefinition.getName(), dataFetcher);
            });
            return typeWiring;
        });
    }

    private String extractCollectionName(FieldDefinition fieldDefinition) {
        TypeName type = fieldDefinition.getType() instanceof ListType
                ? (TypeName) ((ListType) fieldDefinition.getType()).getType()
                : (TypeName) fieldDefinition.getType();
        return type.getName().toLowerCase();
    }

}
