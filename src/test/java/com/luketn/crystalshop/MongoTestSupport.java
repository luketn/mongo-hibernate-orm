package com.luketn.crystalshop;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

final class MongoTestSupport {
    static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:7.0");

    private MongoTestSupport() {
    }

    static AppConfig mongoConfig(MongoDBContainer mongo, String schemaAction, int port) {
        return new AppConfig(mongo.getReplicaSetUrl(), schemaAction, port);
    }

    static MongoClient mongoClient(MongoDBContainer mongo) {
        return MongoClients.create(mongo.getReplicaSetUrl());
    }

    static String databaseName(MongoDBContainer mongo) {
        String database = new ConnectionString(mongo.getReplicaSetUrl()).getDatabase();
        return database == null || database.isBlank() ? "test" : database;
    }
}
