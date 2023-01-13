package rewrite;

import mindustry.game.Team;

import static rewrite.Main.fractions;

public class Fraction {

    public final Team team;
    public final int id;

    public Fraction(Team team) {
        this.team = team;

        this.id = fractions.add(this).size;
    }
}