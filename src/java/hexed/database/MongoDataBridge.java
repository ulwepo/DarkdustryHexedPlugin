package hexed.database;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public abstract class MongoDataBridge<T extends MongoDataBridge<T>> {
    private static MongoCollection<Document> collection;
    private static final Set<String> specialKeys = Set.of("_id", "__v");
    public ObjectId _id;
    public int __v;
    private Map<String, Object> latest = new HashMap<>();

    public MongoDataBridge() {}

    public static void setSourceCollection(MongoCollection<Document> collection) {
        MongoDataBridge.collection = collection;
    }

    public static MongoCollection<Document> getSourceCollection() {
        return collection;
    }

    public void save(final Consumer<Throwable> callback) {
        Map<String, Object> values = this.getDeclaredPublicFields();
        BasicDBObject operations = this.toBsonOperations(this.latest, values);
        if (!operations.isEmpty()) {
            this.latest = values;
            collection.findOneAndUpdate(new BasicDBObject("_id", values.get("_id")), operations, (new FindOneAndUpdateOptions()).upsert(true).returnDocument(ReturnDocument.AFTER)).subscribe(new Subscriber<>() {
                public void onSubscribe(Subscription s) {
                    s.request(1L);
                }

                public void onNext(Document t) {
                }

                public void onComplete() {
                    callback.accept(null);
                }

                public void onError(Throwable t) {
                    if (!Objects.isNull(t)) {
                        callback.accept(t);
                    }

                }
            });
        }
    }

    public void save() {
        this.save((error) -> {
            if (!Objects.isNull(error)) {
                error.printStackTrace();
            }

        });
    }

    public void resetLatest() {
        this.latest = this.getDeclaredPublicFields();
    }

    public static <T extends MongoDataBridge<T>> void find(Class<T> clazz, BasicDBObject filter, final Consumer<T> callback) {
        try {
            final T dataClass = clazz.getConstructor().newInstance();
            final Set<Field> fields = Set.of(clazz.getFields());
            Document defaultObject = new Document();
            fields.forEach((field) -> {
                if (!specialKeys.contains(field.getName())) {
                    try {
                        defaultObject.append(field.getName(), field.get(dataClass));
                    } catch (IllegalAccessException | IllegalArgumentException var4) {
                        var4.printStackTrace();
                    }
                }

            });
            filter.forEach(defaultObject::append);
            collection.findOneAndUpdate(filter, new BasicDBObject("$setOnInsert", defaultObject), (new FindOneAndUpdateOptions()).upsert(true).returnDocument(ReturnDocument.AFTER)).subscribe(new Subscriber<>() {
                public void onSubscribe(Subscription s) {
                    s.request(1L);
                }

                public void onNext(Document document) {
                    fields.forEach((field) -> {
                        try {
                            field.set(dataClass, document.getOrDefault(field.getName(), field.get(dataClass)));
                        } catch (IllegalAccessException | IllegalArgumentException var4) {
                            var4.printStackTrace();
                        }

                    });
                    dataClass.resetLatest();
                    callback.accept(dataClass);
                }

                public void onComplete() {
                }

                public void onError(Throwable t) {
                    if (!Objects.isNull(t)) {
                        t.printStackTrace();
                    }

                }
            });
        } catch (Exception var6) {
            var6.printStackTrace();
        }

    }

    private Map<String, Object> getDeclaredPublicFields() {
        Field[] fields = this.getClass().getFields();
        Map<String, Object> values = new HashMap<>();

        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers())) {
                try {
                    values.put(field.getName(), field.get(this));
                } catch (Exception ignored) {
                }
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
