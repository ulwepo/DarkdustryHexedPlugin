package hexed.database;

import java.util.*;

import com.mongodb.reactivestreams.client.MongoCollection;

import org.bson.Document;
import org.bson.types.ObjectId;

import hexed.database.events.OnComplete;
import hexed.database.events.OnError;
import hexed.database.events.OnNext;
import hexed.database.events.OnSubscribe;

public class MongoSchema<R, N> extends MongoEvents {
    public HashSet<MongoAccessor<?>> schema = new HashSet<>();
    
    private final MongoCollection<Document> collection;

    public MongoSchema(MongoCollection<Document> collection, MongoAccessor<?>... accessors) {
        super();
        
        this.collection = collection;
        schema.addAll(Arrays.asList(accessors));

        this.<OnSubscribe>addListener(OnSubscribe.class, (subscriber) -> subscriber.subscription.request(1));
    }

    public Document create(Map<String, Object> data) {
        data.forEach((key, value) -> {
            Iterator<MongoAccessor<?>> iterableSchema = schema.iterator();
            MongoAccessor<?> accessor = null;

            while (iterableSchema.hasNext()) {
                MongoAccessor<?> valueAccessor = iterableSchema.next();
                if (!valueAccessor.getKey().equals(key)) continue;
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

        return insertDocument;
    }

    public void insertDoc(Document insertDocument) {
        Document checkedDocument = applySchema(insertDocument);

        this.collection.insertOne(checkedDocument).subscribe(new ArrowSubscriber<>(
            subscribe -> this.fireEvent(OnSubscribe.class, subscribe),
            next -> this.fireEvent(OnNext.class, next),
            complete -> this.fireEvent(OnComplete.class, complete),
            error -> this.fireEvent(OnError.class, error)
        ));
    }

    public Document applySchema(Document document) throws IllegalArgumentException {
        Document newDocument = new Document()
            .append("_id", document.get("_id"))
            .append("__v", document.get("__v"));
        this.schema.forEach((accessor) -> {
            String accessorKey = accessor.getKey();
            Object documentData = document.get(accessorKey);
            
            if (accessor.isValidData(documentData == null ? null : documentData.getClass())) {
                newDocument.append(accessorKey, documentData);
                return;
            }

            if (accessor instanceof NonRequired<?> nonrequired) {
                if (nonrequired.hasDefault()) newDocument.append(accessorKey, nonrequired.getDefaultValue());
                return;
            }

            throw new IllegalArgumentException("Невозможно пропарсить ключ \"" + accessorKey + "\", который является обязательным\n" + document.toJson());
        });

        return newDocument;
    }

    public boolean canApplySchema(Document document) {
        try {
            this.applySchema(document);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Document tryApplySchema(Document document) {
        try {
            return this.applySchema(document);
        } catch (Exception e) {
            return null;
        }
    }
}
