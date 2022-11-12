package hexed.components;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonWriter.OutputType;

import static mindustry.Vars.dataDirectory;

public class Statistics {

    public static final Json json = new Json();
    public static final Fi file = dataDirectory.child("statistics.json");

    public static ObjectMap<String, PlayerData> datas;

    @SuppressWarnings("unchecked")
    public static void load() {
        json.addClassTag("PlayerData", PlayerData.class);
        json.setOutputType(OutputType.json);

        datas = file.exists() ? json.fromJson(ObjectMap.class, file) : new ObjectMap<>();
    }

    public static PlayerData getData(String uuid) {
        return datas.get(uuid, PlayerData::new);
    }

    public static void save() {
        json.toJson(datas, file);
    }

    public static Seq<PlayerData> getLeaders() {
        return Seq.with(datas.values())
                .filter(data -> data.wins > 0)
                .sort(data -> -data.wins);
    }

    public static class PlayerData {
        public String name;
        public int wins;
    }
}