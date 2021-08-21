package hexed.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoCollection;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import hexed.database.events.OnComplete;
import hexed.database.events.OnError;
import hexed.database.events.OnNext;
import hexed.database.events.OnSubscribe;

public class MongoSchema<R, N> extends MongoEvents {
    public HashSet<MongoAccessor<?>> schema = new HashSet<>();
    
    private MongoCollection<Document> collection;

    public MongoSchema(MongoCollection<Document> collection, MongoAccessor<?>... accessors) {
        super();
        
        this.collection = collection;
        for (MongoAccessor<?> anyValue : accessors) {
            schema.add(anyValue);
        }

        this.<OnSubscribe>addListener(OnSubscribe.class, (subscriber) -> {
            subscriber.subscription.request(1);
        });
    }

    public Document create(Map<String, Object> data) {
        data.forEach((key, value) -> {
            Iterator<MongoAccessor<?>> iterableSchema = schema.iterator();
            MongoAccessor<?> accessor = null;

            while (iterableSchema.hasNext()) {
                MongoAccessor<?> valueAccessor = iterableSchema.next();
                if (!((MongoAccessor<?>) valueAccessor).getKey().equals(key)) continue;
                accessor = valueAccessor;
                break;
            }
            
            if (accessor == null)
                throw new IllegalArgumentException("Схема не содержит ключа \"" + key + "\"");
            if (value.getClass() != accessor.getDataClass())
                throw new IllegalArgumentException("Значение \"" + value + "\" равно " + value.getClass().getName() + ", ожидалось " + accessor.getDataClass().getName());
        });

        this.schema.forEach((accessor) -> {
            String accessorKey = accessor.getKey();
            Class<?> checkClass = !data.containsKey(accessorKey) ? null : data.get(accessorKey).getClass();
            if (!accessor.isValidData(checkClass))
                throw new IllegalArgumentException("Ключ \"" + accessorKey + "\" был объявлен, как обязательный, но не был инициализирован в документе!");
        });

        HashMap<String, Object> insertMap = new HashMap<>(Map.<String, Object>of(
            "_id", new ObjectId(),
            "__v", 0
        ));

        insertMap.putAll(data);
        Document insertDocument = new Document(insertMap);
        MongoSchema<R, N> self = this;
        
        this.collection.insertOne(insertDocument).subscribe(new Subscriber<InsertOneResult>() {
            @Override
            public void onNext(InsertOneResult t) {
                self.fireEvent(OnNext.class, t);
            }

            @Override
            public void onSubscribe(Subscription s) {
                self.fireEvent(OnSubscribe.class, s);
            }

            @Override
            public void onComplete() {
                self.fireEvent(OnComplete.class);
            }

            @Override
            public void onError(Throwable t) {
                self.fireEvent(OnError.class, t);
            }
        });

        return insertDocument;
    }
}
