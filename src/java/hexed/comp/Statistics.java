package hexed.comp;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;

public class Statistics {

    public static ObjectMap<String, PlayerData> datas;

    @SuppressWarnings("unchecked")
    public static void load() {
        datas = Core.settings.getJson("statistics", ObjectMap.class, ObjectMap::new);
    }

    public static PlayerData getData(String uuid) {
        return datas.get(uuid, PlayerData::new);
    }

    public static void save() {
        Core.settings.putJson("statistics", ObjectMap.class, datas);
    }

    public static Seq<PlayerData> getLeaders() {
        return datas.values().toSeq().filter(data -> data.wins != 0).sort(data -> -data.wins);
    }
}
