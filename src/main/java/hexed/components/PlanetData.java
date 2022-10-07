package hexed.components;

import arc.struct.Seq;
import mindustry.game.Schematic;
import mindustry.type.ItemStack;
import mindustry.world.Block;

public record PlanetData(Seq<Block> ores, Seq<ItemStack> loadout, Schematic startScheme) {}