package hexed;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

import mindustry.net.Administration;
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

        collection.find(new BasicDBObject("port", Administration.Config.port.num())).subscribe(new ArrowSubscriber<>(
            subscribe -> subscribe.request(1),
            next -> {
                if (next == null) {
                    next = statistics.create(Administration.Config.port.num(), "I DONT KNOOOOWWWW", "{}");
                }

                Document statisticsDocument = statistics.tryApplySchema(next);

                if (statisticsDocument == null) {
                    collection
                        .findOneAndDelete(new BasicDBObject("_id", next.getObjectId("_id")))
                        .subscribe(new ArrowSubscriber<>());
                    next = statistics.create(Administration.Config.port.num(), "I DONT KNOOOOWWWW", "{}");
                }

                next.replace("serverSharedData", collection.toString());
                collection
                    .findOneAndReplace(new Document("_id", next.getObjectId("_id")), next)
                    .subscribe(new ArrowSubscriber<>());
            },
            null,
            null
        ));
    }
}
