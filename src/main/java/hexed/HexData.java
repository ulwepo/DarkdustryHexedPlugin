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
    public static final Seq<PlayerData> datas = new Seq<>();
    /** All hexes on the map. No order. */
    public static final Seq<Hex> hexes = new Seq<>();

    /** Maps team ID -> player data */
    private static final IntMap<PlayerData> teamData = new IntMap<>();
    /** Maps team ID -> player */
    private static final IntMap<Player> teamPlayer = new IntMap<>();

    public static void init() {
        datas.clear();
        hexes.clear();

        HexedGenerator.getHexes((x, y) -> hexes.add(new Hex(hexes.size, x, y)));
        Groups.player.each(player -> datas.add(new PlayerData(player)));
    }

    public static void updateTeamMaps() {
        teamData.clear();
        datas.each(data -> {
            teamData.put(data.player.team().id, data);
        });

        teamPlayer.clear();
        Groups.player.each(player -> {
            teamPlayer.put(player.team().id, player);
        });
    }

    public static void updateControl() {
        hexes.each(Hex::updateController);
    }

    public static Seq<PlayerData> getLeaderboard() {
        return datas.copy().filter(data -> data.controls() > 0).sort(data -> -data.controls());
    }

    public static Player getPlayer(Team team) {
        return teamPlayer.get(team.id);
    }

    public static PlayerData getData(Team team) {
        return teamData.get(team.id);
    }

    public static PlayerData getData(String uuid) {
        return datas.find(data -> data.player.uuid().equals(uuid));
    }

    public static int getControlledSize(Player player) {
        return teamData.get(player.team().id).controls();
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
