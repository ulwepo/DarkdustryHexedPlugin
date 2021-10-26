package org.darkdustry.mindustry.hexed.models;

import com.mongodb.BasicDBObject;
import java.util.function.Consumer;
import org.darkdustry.MongoDataBridge;

public class UserStatistics extends MongoDataBridge<UserStatistics> {

    public String UUID;
    public String name = "";
    public int wins = 0;

    public static void find(
        BasicDBObject filter,
        Consumer<UserStatistics> callback
    ) {
        UserStatistics.find(UserStatistics.class, filter, callback);
    }
}
