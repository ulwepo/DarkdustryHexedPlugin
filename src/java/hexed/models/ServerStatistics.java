package hexed.models;

import java.util.Map;

import com.mongodb.reactivestreams.client.MongoCollection;

import org.bson.Document;

import hexed.database.MongoSchema;
import hexed.database.Required;

public class ServerStatistics extends MongoSchema<String, Object> {
    public ServerStatistics(MongoCollection<Document> collection) {
        super(
            collection,
            new Required<>("port", Integer.class),
            new Required<>("name", String.class),
            new Required<>("serverSharedData", String.class)
        );
    }

    public Document create(Integer port, String name, String serverSharedData) {
        return this.create(Map.of(
            "port", port,
            "name", name,
            "serverSharedData", serverSharedData
        ));
    }
}
