package rewrite;

import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Player;

public class HexTeam {

    public static final Seq<HexTeam> teams = new Seq<>();

    public Player player;
    public int id;

    public static HexTeam get(Player player) {
        return teams.find(team -> team.player.uuid().equals(player.uuid()));
    }

    public HexTeam(Player player) {
        this.player = player;
        this.id = teams.add(this).size;
    }
}