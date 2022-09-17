package hexed.generation;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.maps.filters.*;
import mindustry.type.Weather.WeatherEntry;

import static hexed.Main.*;

public class GenerationTypes {

    public static GenerationType beta;

    public static void load() {
        beta = new GenerationType("[white]\uF7EA [accent]Beta", Planets.serpulo, Blocks.moss, Blocks.sporeWall, rules -> {
            rules.lighting = true;
            rules.ambientLight = Color.black.cpy().a(.7f);

            rules.weather.add(new WeatherEntry(Weathers.sporestorm, 5f, 10f, 1f, 2f) {{
                intensity = .25f;
            }});
        },
                new NoiseFilter() {{
                   floor = Blocks.grass;
                   block = Blocks.shrubs;
                }},
                new NoiseFilter() {{
                   floor = Blocks.sand;
                   block = Blocks.sandWall;
                }},
                new NoiseFilter() {{
                   floor = Blocks.darksand;
                   block = Blocks.duneWall;
                }}
        );
    }

    public static Seq<GenerationType> all() {
        return Seq.with(beta);
    }

    public static GenerationType random() {
        return all().random(type);
    }
}
