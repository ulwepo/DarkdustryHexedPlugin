package hexed.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MongoEvents {
    public Map<Class<?>, HashSet<Callback<Object>>> listeners = new HashMap<>();

    public <T extends Object> void addListener(Class<?> event, Callback<T> listener) {
        if (listeners.get(event) == null)
            listeners.put(event, new HashSet<Callback<Object>>());
        listeners.get(event).add((Callback<Object>) listener);
    }

    public void fireEvent(Class<?> event, Object... callParams) throws IOException {
        if (!listeners.containsKey(event)) return;
        listeners.get(event).forEach((listener) -> {
            try {
                listener.call(event.getDeclaredConstructors()[0].newInstance(callParams));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}