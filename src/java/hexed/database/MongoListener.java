package hexed.database;

@FunctionalInterface
public interface MongoListener<T> {
    void call(T eventData);
}