package hexed.generation;

import arc.graphics.Color;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.maps.filters.*;
import mindustry.type.Weather.WeatherEntry;

public class GenerationTypes {

    public static GenerationType beta;

    public static void load() {
        beta = new GenerationType("beta", Planets.serpulo, rules -> {
            rules.lighting = true;
            rules.ambientLight = Color.grays(.9f);

            rules.weather.add(new WeatherEntry(Weathers.sporestorm, 10f, 30f, 1f, 2f) {{
                intensity = .25f;
            }});
        },
                new NoiseFilter() {{
                    floor = Blocks.grass;
                    block = Blocks.air;
                }},
                new NoiseFilter() {{
                    floor = Blocks.sand;
                    block = Blocks.air;
                }},
                new NoiseFilter() {{
                    floor = Blocks.darksand;
                    block = Blocks.air;
                }},
                new ClearFilter() {{
                    target = Blocks.stone;
                    replace = Blocks.moss;
                }}
        );
    }
}
