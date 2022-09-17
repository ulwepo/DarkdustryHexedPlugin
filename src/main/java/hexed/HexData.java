package hexed;

import arc.math.geom.Point2;
import arc.math.geom.Position;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class HexData {

    /** All hexes on the map. No order. */
    private static final Seq<Hex> hexes = new Seq<>();
    /** Maps team ID -> player */
    private static final IntMap<Player> teamMap = new IntMap<>();
    /** Maps team ID -> list of controlled hexes */
    private static final IntMap<Seq<Hex>> control = new IntMap<>();

    public static void updateStats() {
        teamMap.clear();
        control.clear();

        Groups.player.each(player -> teamMap.put(player.team().id, player));

        hexes.each(hex -> hex.controller != null, hex -> control.get(hex.controller.id, Seq::new).add(hex));
    }

    public static void updateControl() {
        hexes.each(Hex::updateController);
    }

    public static Seq<Player> getLeaderboard() {
        Seq<Player> players = Groups.player.copy(new Seq<>());
        return players.filter(p -> getControlled(p).size > 0).sort(p -> -getControlled(p).size);
    }

    public static Player getPlayer(Team team) {
        return teamMap.get(team.id);
    }

    public static Seq<Hex> getControlled(Player player) {
        return getControlled(player.team());
    }

    public static Seq<Hex> getControlled(Team team) {
        return control.get(team.id, Seq::new);
    }

    public static int getControlledSize(Player player) {
        return getControlledSize(player.team());
    }

    public static int getControlledSize(Team team) {
        return getControlled(team).size;
    }

    public static void initHexes() {
        HexedGenerator.getHexes((x, y) -> hexes.add(new Hex(hexes.size, x, y)));
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
}
