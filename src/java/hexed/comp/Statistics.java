package hexed.comp;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Json;

import static mindustry.Vars.*;

public class Statistics {

    public static ObjectMap<String, PlayerData> datas;

    public static Json json = new Json();
    public static Fi file = dataDirectory.child("statistics.json");

    @SuppressWarnings("unchecked")
    public static void load() {
        json.addClassTag("hexed.comp.PlayerData", PlayerData.class); // rly important thing
        datas = file.exists() ? json.fromJson(ObjectMap.class, file) : new ObjectMap<>();
    }

    public static PlayerData getData(String uuid) {
        return datas.get(uuid, PlayerData::new);
    }

    public static void save() {
        json.toJson(datas, file);
    }

    public static Seq<PlayerData> getLeaders() {
        return datas.values().toSeq().filter(data -> data.wins > 0).sort(data -> -data.wins);
    }
}
