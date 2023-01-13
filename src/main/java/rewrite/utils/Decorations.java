package rewrite.utils;

import arc.struct.ObjectMap;
import mindustry.content.Blocks;
import mindustry.world.blocks.environment.*;

public class Decorations {

    public static ObjectMap<Floor, SteamVent> vents;
    public static ObjectMap<Floor, TallBlock> crystals;

    public static void load() {
        vents = ObjectMap.of(
                Blocks.rhyolite, Blocks.rhyoliteVent,
                Blocks.beryllicStone, Blocks.arkyicVent,
                Blocks.arkyicStone, Blocks.arkyicVent,
                Blocks.yellowStone, Blocks.yellowStoneVent,
                Blocks.yellowStonePlates, Blocks.yellowStoneVent,
                Blocks.regolith, Blocks.yellowStoneVent,
                Blocks.redStone, Blocks.redStoneVent,
                Blocks.denseRedStone, Blocks.redStoneVent,
                Blocks.carbonStone, Blocks.carbonVent
        );

        crystals = ObjectMap.of(
                Blocks.crystallineStone, Blocks.crystalCluster,
                Blocks.crystalFloor, Blocks.vibrantCrystalCluster,
                Blocks.regolith, Blocks.crystalBlocks,
                Blocks.arkyicStone, Blocks.crystalOrbs
        );
    }
}