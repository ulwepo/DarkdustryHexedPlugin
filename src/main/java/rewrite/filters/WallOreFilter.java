package rewrite.filters;

import mindustry.maps.filters.OreFilter;

public class WallOreFilter extends OreFilter {

    public boolean wallOre;

    @Override
    public void apply(GenerateInput in) {
        if (wallOre ? !in.block.solid : !in.floor.asFloor().hasSurface()) return;

        float noise = noise(in.x, in.y + in.x * tilt, scl, 1f, octaves, falloff);
        if (noise <= threshold) return;

        if (target.isAir() || in.floor == target || in.block == target || in.overlay == target)
            in.overlay = ore;
    }
}