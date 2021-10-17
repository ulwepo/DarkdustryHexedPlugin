package hexed;

import java.util.HashSet;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import hexed.database.ArrowSubscriber;

public class Main {
    public static void main(String[] args) {
        ConnectionString connString = new ConnectionString("mongodb+srv://host:BmnP4NEpht8wQFqv@darkdustry.aztzv.mongodb.net");
        
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connString)
            .retryWrites(true)
            .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("darkdustry");
        MongoCollection<Document> collection = database.getCollection("hexed");

        // ServerStatistics statistics = new ServerStatistics(collection);
        HashSet<Document> players = new HashSet<>();

        collection
            .find()
            .sort(
                new BsonDocument(
                    "wins",
                    new BsonInt32(-1)
                )
            )
            .limit(10)
            .subscribe(new ArrowSubscriber<Document>(
                subscribe -> subscribe.request(10),
                players::add,
                completed -> {
                    System.out.println(players.toArray()[0]);
                }, null)
            );
    }
}
