package com.altair.graphql.config;


import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.springframework.config.ArangoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArangoDbConfig implements ArangoConfiguration {


    public ArangoDatabase arangoDatabase(){
        return arango().build().db("aone");
    }

    @Override
    public ArangoDB.Builder arango() {
        return new ArangoDB.Builder().host("localhost", 8529).user("root").password("roo");
    }

    @Override
    public String database() {
        return "mydb";
    }


}

