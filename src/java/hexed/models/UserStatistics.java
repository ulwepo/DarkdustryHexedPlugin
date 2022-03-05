package hexed.models;

import arc.func.Cons;
import com.mongodb.BasicDBObject;
import hexed.database.MongoDataBridge;
import mindustry.gen.Player;

public class UserStatistics extends MongoDataBridge<UserStatistics> {

    public String UUID;
    public String name = "";
    public int wins = 0;

    public static void find(Player player, Cons<UserStatistics> cons) {
        if (player != null && !player.isLocal()) find(player.uuid(), cons);
    }

    public static void find(String UUID, Cons<UserStatistics> cons) {
        findAndApplySchema(UserStatistics.class, new BasicDBObject("UUID", UUID), cons);
    }
}
