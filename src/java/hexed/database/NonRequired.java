package hexed.database;

public class NonRequired<T> extends MongoAccessor<T> {
    public NonRequired(String key, Class<T> valueClass) {
        super(key, valueClass);
    }

    @Override
    public boolean isValidData(Class<?> valueClass) {
        return true;
    }
}
