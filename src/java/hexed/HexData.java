package hexed;

import arc.Events;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class HexData {

    /** All hexes on the map. No order. */
    private final Seq<Hex> hexes = new Seq<>();
    /** Maps world pos -> hex */
    private final IntMap<Hex> hexPos = new IntMap<>();
    /** Maps team ID -> player */
    private final IntMap<Player> teamMap = new IntMap<>();
    /** Maps team ID -> list of controlled hexes */
    private final IntMap<Seq<Hex>> control = new IntMap<>();
    /** Data of specific teams. */
    private final HexTeam[] teamData = new HexTeam[256];

    public void updateStats() {
        teamMap.clear();
        control.clear();

        Groups.player.each(player -> teamMap.put(player.team().id, player));

        Groups.player.each(player -> !player.dead(), player -> {
            HexTeam team = data(player);
            Hex newHex = hexes.min(hex -> player.dst2(hex.wx, hex.wy));
            if (team.location != newHex) {
                team.location = newHex;
                team.progressPercent = newHex.getProgressPercent(player.team());
                team.lastCaptured = newHex.controller == player.team();
                Events.fire(new HexMoveEvent(player, newHex));
            }

            float currPercent = newHex.getProgressPercent(player.team());
            if (team.progressPercent != currPercent) {
                team.progressPercent = currPercent;
                Events.fire(new ProgressIncreaseEvent(player, currPercent));
            }

            boolean captured = newHex.controller == player.team();
            if (team.lastCaptured != captured) {
                team.lastCaptured = captured;
                if (captured && !newHex.hasCore()) Events.fire(new HexCaptureEvent(player, newHex));
            }
        });

        hexes.each(hex -> hex.controller != null, hex -> control.get(hex.controller.id, Seq::new).add(hex));
    }

    public void updateControl() {
        hexes.each(Hex::updateController);
    }

    public Seq<Player> getLeaderboard() {
        Seq<Player> players = Groups.player.copy(new Seq<>());
        return players.filter(p -> getControlled(p).size > 0).sort(p -> -getControlled(p).size);
    }

    public Player getPlayer(Team team) {
        return teamMap.get(team.id);
    }

    public Seq<Hex> getControlled(Player player) {
        return getControlled(player.team());
    }

    public Seq<Hex> getControlled(Team team) {
        return control.get(team.id, Seq::new);
    }

    public int getControlledSize(Player player) {
        return getControlledSize(player.team());
    }

    public int getControlledSize(Team team) {
        return getControlled(team).size;
    }

    public void initHexes(IntSeq ints) {
        for (int i = 0; i < ints.size; i++) {
            int pos = ints.get(i);
            hexes.add(new Hex(i, Point2.x(pos), Point2.y(pos)));
            hexPos.put(pos, hexes.peek());
        }
    }

    public Seq<Hex> hexes() {
        return hexes;
    }

    public Hex getSpawnHex() {
        return hexes.copy().shuffle().find(hex -> hex.controller == null && hex.spawnTime.get());
    }

    public Hex getHex(int position) {
        return hexPos.get(position);
    }

    public HexTeam data(Team team) {
        if (teamData[team.id] == null) teamData[team.id] = new HexTeam();
        return teamData[team.id];
    }

    public HexTeam data(Player player) {
        return data(player.team());
    }

    public static class HexTeam {
        public boolean dying;
        public boolean chosen;

        public Hex location;

        public float progressPercent;
        public boolean lastCaptured;
    }

    public record HexCaptureEvent(Player player, Hex hex) {}

    public record HexMoveEvent(Player player, Hex hex) {}

    public record ProgressIncreaseEvent(Player player, float percent) {}
}
