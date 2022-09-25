package hexed.components;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.Json.JsonSerializable;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter.OutputType;

import static mindustry.Vars.dataDirectory;

public class Statistics {

    public static final Json json = new Json();
    public static final Fi statistics = dataDirectory.child("statistics.json");

    public static ObjectMap<String, PlayerData> datas;

    @SuppressWarnings("unchecked")
    public static void load() {
        json.addClassTag("PlayerData", PlayerData.class);
        json.setOutputType(OutputType.json);

        if (statistics.exists()) { // переносим и упрощаем данные
            String data = statistics.readString();
            data = data.replaceAll("hexed.comp.PlayerData", "PlayerData");
            statistics.writeString(data, false);
        }

        datas = statistics.exists() ? json.fromJson(ObjectMap.class, statistics) : new ObjectMap<>();
    }

    public static PlayerData getData(String uuid) {
        return datas.get(uuid, PlayerData::new);
    }

    public static void save() {
        json.toJson(datas, statistics);
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
