package hexed.models;

import java.util.Map;

import com.mongodb.reactivestreams.client.MongoCollection;

import org.bson.Document;

import hexed.database.MongoSchema;
import hexed.database.Required;

public class UserStatistics extends MongoSchema<String, Object> {
    public UserStatistics(MongoCollection<Document> collection) {
        super(
            collection,
            new Required<>("wins", Integer.class),
            new Required<>("name", String.class),
            new Required<>("UUID", String.class)
        );
    }

    public Document create(Integer wins, String name, String UUID) {
        return this.create(Map.of(
            "wins", wins,
            "name", name,
            "UUID", UUID
        ));
    }
}
