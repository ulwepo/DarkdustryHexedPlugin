package hexed;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

import org.bson.Document;

import hexed.database.ArrowSubscriber;
import hexed.models.ServerStatistics;

public class Main {
    public static void main(String[] args) {
        ConnectionString connString = new ConnectionString("mongodb+srv://host:BmnP4NEpht8wQFqv@darkdustry.aztzv.mongodb.net");
        
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connString)
            .retryWrites(true)
            .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("darkdustry");
        MongoCollection<Document> collection = database.getCollection("statistics");

        ServerStatistics statistics = new ServerStatistics(collection);

        collection.find(new Document("__v", 1000)).subscribe(new ArrowSubscriber<Document>(
                subscribe -> subscribe.request(1),
                next -> {
                    System.out.println(statistics.canApplySchema(next));
                    System.out.println(statistics.tryApplySchema(next));
                }, null, null
            )
        );
    }
}
