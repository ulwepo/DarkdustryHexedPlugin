package hexed.database;

@FunctionalInterface
public interface Callback<T> {
    void call(T callData) throws Exception;
}