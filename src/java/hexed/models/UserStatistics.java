package hexed.models;

import com.mongodb.BasicDBObject;
import hexed.database.MongoDataBridge;

import java.util.function.Consumer;

public class UserStatistics extends MongoDataBridge<UserStatistics> {

    public String UUID;
    public String name = "";
    public int wins = 0;

    public static void find(BasicDBObject filter, Consumer<UserStatistics> callback) {
        UserStatistics.find(UserStatistics.class, filter, callback);
    }
}
