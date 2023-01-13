package rewrite.world;

import mindustry.content.Blocks;
import mindustry.world.Block;

public class Generators {

    public static Generator serpulo, erekir;

    public static void load() {
        serpulo = new SerpuloGenerator("Serpulo", new Block[][] {
                {Blocks.stone}
        }, rules -> {

        }) {{

        }};

        erekir = new ErekirGenerator("Erekir", new Block[][] {
                {Blocks.carbonStone, Blocks.arkyicStone, Blocks.crystallineStone, Blocks.denseRedStone, Blocks.rhyolite},
                {Blocks.beryllicStone, Blocks.rhyolite, Blocks.redmat, Blocks.regolith, Blocks.redmat},
                {Blocks.arkyicStone, Blocks.beryllicStone, Blocks.beryllicStone, Blocks.beryllicStone, Blocks.bluemat},
                {Blocks.regolith, Blocks.beryllicStone, Blocks.crystalFloor, Blocks.ferricStone, Blocks.roughRhyolite},
                {Blocks.ferricStone, Blocks.ferricCraters, Blocks.regolith, Blocks.ferricStone, Blocks.regolith}
        }, rules -> {

        });
    }
}