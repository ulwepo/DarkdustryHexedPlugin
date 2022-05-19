package hexed.comp;

import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.Json.JsonSerializable;

public class PlayerData implements JsonSerializable {

    public String name;
    public int wins;

    public void write(Json json) {
        json.writeValue("name", name);
        json.writeValue("wins", wins);
    }

    public void read(Json json, JsonValue data) {
        name = data.getString("name");
        wins = data.getInt("wins");
    }
}
