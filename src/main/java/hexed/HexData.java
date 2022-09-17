package hexed;

import arc.math.geom.Position;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Timer.Task;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class HexData {

    /** Data of all players, including not connected. */
    private static final IntMap<PlayerData> datas = new IntMap<>();
    /** All hexes on the map. No order. */
    private static final Seq<Hex> hexes = new Seq<>();
    /** Maps team ID -> player */
    private static final IntMap<Player> teamMap = new IntMap<>();

    public static void init() {
        datas.clear();
        hexes.clear();

        HexedGenerator.getHexes((x, y) -> hexes.add(new Hex(hexes.size, x, y)));
        Groups.player.each(player -> new PlayerData(player));
    }

    public static void updateTeamMap() {
        teamMap.clear();
        Groups.player.each(player -> teamMap.put(player.team().id, player));
    }

    public static void updateControl() {
        hexes.each(Hex::updateController);
    }

    public static Seq<PlayerData> getLeaderboard() {
        return datas.values().toArray().filter(data -> data.controls() > 0).sort(data -> -data.controls());
    }

    public static Player getPlayer(Team team) {
        return teamMap.get(team.id);
    }

    public static PlayerData getData(Team team) {
        return datas.get(getPlayer(team).id);
    }

    public static int getControlledSize(Player player) {
        return datas.get(player.id).controls();
    }

    public static int hexesAmount() {
        return hexes.size;
    }

    public static Hex getSpawnHex() {
        return hexes.copy().shuffle().find(hex -> hex.controller == null);
    }

    public static Hex getHex(Position position) {
        return hexes.find(hex -> hex.contains(position));
    }

    public static class PlayerData {

        public Player player;
        public Seq<Hex> controlled = new Seq<>();

        public Task left;

        public PlayerData(Player player) {
            this.setPlayer(player);
        }

        public void setPlayer(Player player) {
            datas.remove(this.player.id);
            datas.put(player.id, this);

            this.player = player;
        }

        public String name() {
            return player.coloredName();
        }

        public int controls() {
            return controlled.size;
        }
    }
}
