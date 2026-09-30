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

    private record VisualKey(BlockPos position, Direction side) {}
    private static final java.util.WeakHashMap<Level,VisualCache> visualCaches=new java.util.WeakHashMap<>();
    private static final class VisualCache {long tick=Long.MIN_VALUE;final java.util.HashMap<VisualKey,ShaftPower> values=new java.util.HashMap<>();}
    /** Display-only memoization expires each tick; live gameplay resolution remains uncached. */
    static ShaftPower resolveVisual(Level level,BlockPos position,Direction side) {
        if(level.isClientSide)return ShaftPower.STOPPED;
        var cache=visualCaches.computeIfAbsent(level,key->new VisualCache());
        if(cache.tick!=level.getGameTime() || cache.values.size()>4096){cache.values.clear();cache.tick=level.getGameTime();}
        var keys=new ArrayList<VisualKey>();var steps=new ArrayList<ShaftNode>();var visited=new HashSet<VisualKey>();
        ShaftPower power=ShaftPower.STOPPED;
        for(int count=0;count<MAX_NODES;count++) {
            var key=new VisualKey(position.immutable(),side);
            if(!level.hasChunkAt(position) || !visited.add(key))return ShaftPower.STOPPED;
            var known=cache.values.get(key);if(known!=null){power=known;break;}
            ShaftNode node=level.getCapability(CAPABILITY,position,side);if(node==null)return ShaftPower.STOPPED;
            keys.add(key);steps.add(node);var input=node.input();
            if(input==null)break;
            if(count==MAX_NODES-1)return ShaftPower.STOPPED;
            position=input.position();side=input.outputSide();
        }
        for(int index=steps.size()-1;index>=0;index--){power=steps.get(index).apply(power);cache.values.put(keys.get(index),power);}
        return power;
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
