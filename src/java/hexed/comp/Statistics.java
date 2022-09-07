package hexed.comp;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.Json.JsonSerializable;
import mindustry.io.JsonIO;

import static mindustry.Vars.*;

public class Statistics {

    public static ObjectMap<String, PlayerData> datas;

    public static Fi statistics = dataDirectory.child("statistics.json");

    @SuppressWarnings("unchecked")
    public static void load() {
        JsonIO.json.addClassTag("hexed.comp.PlayerData", PlayerData.class); // rly important thing
        datas = statistics.exists() ? JsonIO.json.fromJson(ObjectMap.class, statistics) : new ObjectMap<>();
    }

    public static PlayerData getData(String uuid) {
        return datas.get(uuid, PlayerData::new);
    }

    public static void save() {
        JsonIO.json.toJson(datas, statistics);
    }

    public static Seq<PlayerData> getLeaders() {
        return datas.values().toSeq()
                .filter(data -> data.wins > 0)
                .sort(data -> -data.wins);
    }

    public static class PlayerData implements JsonSerializable {

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
}
