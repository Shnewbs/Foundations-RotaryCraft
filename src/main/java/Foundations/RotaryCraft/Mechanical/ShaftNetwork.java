package Foundations.RotaryCraft.Mechanical;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** Pulls live signals without ticking, buffering energy, recursion or loading chunks. */
public final class ShaftNetwork {
    public static final BlockCapability<ShaftNode, Direction> CAPABILITY = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath("rotarycraft", "shaft_power"), ShaftNode.class);
    public static final int MAX_NODES = 256;
    private ShaftNetwork() {}

    public static ShaftPower resolve(Level level, BlockPos position, Direction outputSide) {
        if (level.isClientSide) return ShaftPower.STOPPED;
        return resolve(position, outputSide, level::hasChunkAt, (pos, side) -> level.getCapability(CAPABILITY, pos, side));
    }

    /** Shared walker; loaded is checked before querying providers. */
    static ShaftPower resolve(BlockPos position, Direction outputSide, java.util.function.Predicate<BlockPos> loaded,
            java.util.function.BiFunction<BlockPos, Direction, ShaftNode> lookup) {
        var visited = new HashSet<BlockPos>();
        var steps = new ArrayList<ShaftNode>();
        BlockPos current = position.immutable();
        Direction side = outputSide;
        for (int count = 0; count < MAX_NODES; count++) {
            if (!loaded.test(current) || !visited.add(current)) return ShaftPower.STOPPED;
            ShaftNode node = lookup.apply(current, side);
            if (node == null) return ShaftPower.STOPPED;
            steps.add(node);
            ShaftNode.Input input = node.input();
            if (input == null) {
                ShaftPower power = ShaftPower.STOPPED;
                for (int index = steps.size() - 1; index >= 0; index--) power = steps.get(index).apply(power);
                return power;
            }
            current = input.position().immutable();
            side = input.outputSide();
        }
        return ShaftPower.STOPPED;
    }
}
