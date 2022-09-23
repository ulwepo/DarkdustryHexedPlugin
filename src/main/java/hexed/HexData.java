package hexed;

import arc.math.geom.Position;
import arc.struct.Seq;
import arc.util.Timer.Task;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class HexData {

    /** Data of all players, including not connected. */
    public static final Seq<PlayerData> datas = new Seq<>();
    /** All hexes on the map. No order. */
    public static final Seq<Hex> hexes = new Seq<>();

    public static void init() {
        datas.clear();
        Groups.player.each(player -> datas.add(new PlayerData(player)));

        hexes.clear();
        HexedGenerator.getHexes((x, y) -> hexes.add(new Hex(hexes.size, x, y)));
    }

    public static void updateControl() {
        hexes.each(Hex::updateController);
    }

    public static Seq<PlayerData> getLeaderboard() {
        return datas.select(data -> data.controlled() > 0).sort(data -> -data.controlled());
    }

    public static Player getPlayer(Team team) {
        return Groups.player.find(player -> player.team() == team);
    }

    public static PlayerData getData(Team team) {
        return datas.find(data -> data.player.team() == team);
    }

    public static PlayerData getData(String uuid) {
        return datas.find(data -> data.player.uuid().equals(uuid));
    }

    public static Hex getSpawnHex() {
        return hexes.copy().shuffle().find(hex -> hex.controller == null);
    }

    public static Hex getClosestHex(Position position) {
        return hexes.min(hex -> position.dst(hex.wx, hex.wy));
    }

    public static class PlayerData {

        public Player player;

        public Task left;

        public PlayerData(Player player) {
            this.player = player;
        }

        public String name() {
            return player.coloredName();
        }

        public boolean active() {
            return player.team() != Team.derelict && player.con.isConnected();
        }

        public int controlled() {
            return hexes.count(hex -> hex.controller == this);
        }
    }
}
