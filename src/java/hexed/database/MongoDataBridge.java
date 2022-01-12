package hexed.database;

import arc.func.Cons;
import arc.util.Log;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class MongoDataBridge<T extends MongoDataBridge<T>> {

    private static final Set<String> specialKeys = Set.of("_id", "__v");
    private static MongoCollection<Document> collection;
    private static Map<String, Object> latest = new HashMap<>();
    public ObjectId _id;
    public int __v;

    public MongoDataBridge() {}

    public static MongoCollection<Document> getSourceCollection() {
        return collection;
    }

    public static void setSourceCollection(MongoCollection<Document> newCollection) {
        collection = newCollection;
    }

    public static <T extends MongoDataBridge<T>> void find(Class<T> sourceClass, BasicDBObject filter, final Cons<T> cons) {
        try {
            final T dataClass = sourceClass.getConstructor().newInstance();
            final Set<Field> fields = Set.of(sourceClass.getFields());
            Document defaultObject = new Document();
            fields.forEach(field -> {
                if (!specialKeys.contains(field.getName())) {
                    try {
                        defaultObject.append(field.getName(), field.get(dataClass));
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        Log.err(e);
                    }
                }
            });
            filter.forEach(defaultObject::append);
            collection.findOneAndUpdate(filter, new BasicDBObject("$setOnInsert", defaultObject), (new FindOneAndUpdateOptions()).upsert(true).returnDocument(ReturnDocument.AFTER)).subscribe(new Subscriber<>() {
                public void onSubscribe(Subscription s) {
                    s.request(1L);
                }

                public void onNext(Document document) {
                    fields.forEach(field -> {
                        try {
                            field.set(dataClass, document.getOrDefault(field.getName(), field.get(dataClass)));
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            Log.err(e);
                        }
                    });
                    dataClass.resetLatest();
                    cons.get(dataClass);
                }

                public void onComplete() {}

                public void onError(Throwable t) {
                    Log.err(t);
                }
            });
        } catch (Exception e) {
            Log.err(e);
        }
    }

    public void save() {
        Map<String, Object> values = this.getDeclaredPublicFields();
        BasicDBObject operations = this.toBsonOperations(this.latest, values);
        if (!operations.isEmpty()) {
            this.latest = values;
            collection.findOneAndUpdate(new BasicDBObject("_id", values.get("_id")), operations, (new FindOneAndUpdateOptions()).upsert(true).returnDocument(ReturnDocument.AFTER)).subscribe(new Subscriber<>() {
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }

                public void onNext(Document t) {}

                public void onComplete() {}

                public void onError(Throwable t) {
                    Log.err(t);
                }
            });
        }
    }

    public void resetLatest() {
        latest = getDeclaredPublicFields();
    }

    private Map<String, Object> getDeclaredPublicFields() {
        Field[] fields = this.getClass().getFields();
        Map<String, Object> values = new HashMap<>();

        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers())) {
                try {
                    values.put(field.getName(), field.get(this));
                } catch (Exception ignored) {}
            }
        }

        return values;
    }

    private BasicDBObject toBsonOperations(Map<String, Object> previousFields, Map<String, Object> newFields) {
        Map<String, DataChanges> changes = DataChanges.getChanges(previousFields, newFields);
        Map<String, BasicDBObject> operations = new HashMap<>();
        changes.forEach((key, changedValues) -> {
            if (!changedValues.current.equals(DataChanges.undefined) && !specialKeys.contains(key)) {
                if (!operations.containsKey("$set")) {
                    operations.put("$set", new BasicDBObject());
                }

                operations.get("$set").append(key, changedValues.current);
            }
        });
        return new BasicDBObject(operations);
    }
}
