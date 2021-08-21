package hexed.database;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MongoEvents {
    public Map<Class<?>, HashSet<MongoListener<Object>>> listeners = new HashMap<>();

    public <T extends Object> void addListener(Class<?> event, MongoListener<T> listener) {
        if (listeners.get(event) == null)
            listeners.put(event, new HashSet<MongoListener<Object>>());
        listeners.get(event).add((MongoListener<Object>) listener);
    }

    public void fireEvent(Class<?> event, Object... callParams) {
        listeners.get(event).forEach((listener) -> {
            try {
                listener.call(event.getDeclaredConstructors()[0].newInstance(callParams));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {
                e.printStackTrace();
            }
        });
    }
}