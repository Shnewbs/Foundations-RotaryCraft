package Foundations.RotaryCraft.Platform;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Bounded, loaded-only discovery shared by connected nodes during a server tick. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "rotarycraft")
public final class CableNetwork {
  private static final int MAX_NODES = 4096;
  private static final Direction[] DIRECTIONS = Direction.values();
  private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

  private record Port(BlockPos pos, Direction side) {}

  private static final class Cache {
    long tick = Long.MIN_VALUE;
    final Map<BlockPos, Network> nodes = new HashMap<>();
    final Map<BlockPos, Budget> spent = new HashMap<>();
  }

  private static final class Budget {
    int used;
  }

  private static final class Network {
    final List<Port> ports = new ArrayList<>();
    final List<PowerEntity> cells = new ArrayList<>();
    int limit = 1000;
    Budget budget = new Budget();
    boolean oversized;
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void unloaded(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
    if (event.getLevel() instanceof Level level) invalidate(level);
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void levelUnloaded(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
    if (event.getLevel() instanceof Level level) CACHES.remove(level);
  }

  static void invalidate(Level level) {
    var cache = CACHES.get(level);
    if (cache != null) cache.nodes.clear();
  }

  static void distribute(Level level, PowerEntity source) {
    var cache = CACHES.computeIfAbsent(level, k -> new Cache());
    if (cache.tick != level.getGameTime()) {
      cache.tick = level.getGameTime();
      cache.nodes.clear();
      cache.spent.clear();
    }
    var network = cache.nodes.get(source.getBlockPos());
    if (network == null) network = discover(level, source, cache);
    if (network.oversized || network.budget.used >= network.limit) return;
    int budget = Math.min(network.limit - network.budget.used, source.rate());
    int size = network.ports.size();
    int start = size == 0 ? 0 : (int) Math.floorMod(level.getGameTime(), size);
    for (int i = 0; i < size && budget > 0; i++) {
      var port = network.ports.get((start + i) % size);
      if (!level.hasChunkAt(port.pos)) continue;
      var target = level.getCapability(Capabilities.Energy.BLOCK, port.pos, port.side);
      if (target == null) continue;
      int moved = EnergyTransfer.transfer(source.energyPort(null), target, budget);
      budget -= moved;
      network.budget.used += moved;
    }
    // Cells hold surplus; stored cell power never circulates through cable buffers.
    for (var cell : network.cells) {
      if (budget <= 0) break;
      if (cell == source || cell.isRemoved() || !level.hasChunkAt(cell.getBlockPos())) continue;
      int limit = budget;
      if (source.isCell()) {
        var from = source.energyPort(null);
        var to = cell.energyPort(null);
        long excess =
            (long) from.getAmountAsInt() * to.getCapacityAsInt()
                - (long) to.getAmountAsInt() * from.getCapacityAsInt();
        limit =
            (int)
                Math.min(
                    budget,
                    Math.max(0, excess / (from.getCapacityAsInt() + to.getCapacityAsInt())));
      }
      int moved = EnergyTransfer.transfer(source.energyPort(null), cell.energyPort(null), limit);
      budget -= moved;
      network.budget.used += moved;
    }
  }

  private static Network discover(Level level, PowerEntity source, Cache cache) {
    var result = new Network();
    var seen = new HashSet<BlockPos>();
    var ports = new LinkedHashSet<Port>();
    var queue = new ArrayDeque<BlockPos>();
    queue.add(source.getBlockPos());
    seen.add(source.getBlockPos());
    while (!queue.isEmpty()) {
      var pos = queue.removeFirst();
      if (!level.hasChunkAt(pos)) continue;
      var entity = level.getBlockEntity(pos);
      if (entity instanceof PowerEntity node) {
        result.limit = Math.min(result.limit, node.rate());
        if (node.isCell()) result.cells.add(node);
      }
      for (var direction : DIRECTIONS) {
        var next = pos.relative(direction);
        if (!level.hasChunkAt(next)) continue;
        var neighbor = level.getBlockEntity(next);
        boolean wire = neighbor instanceof PowerEntity node && node.isWire();
        if (wire) {
          if (seen.add(next)) {
            if (seen.size() > MAX_NODES) {
              result.oversized = true;
              queue.clear();
              break;
            }
            queue.addLast(next);
          }
        } else ports.add(new Port(next, direction.getOpposite()));
      }
    }
    result.ports.addAll(ports);
    // A topology refresh cannot erase energy already transferred this tick.
    // Merged components inherit each prior budget once; splits conservatively
    // inherit the old component's spend until the next tick.
    var previous = Collections.newSetFromMap(new IdentityHashMap<Budget, Boolean>());
    for (var pos : seen) {
      var budget = cache.spent.get(pos);
      if (budget != null) previous.add(budget);
    }
    for (var budget : previous) result.budget.used += budget.used;
    for (var pos : seen) {
      cache.nodes.put(pos, result);
      cache.spent.put(pos, result.budget);
    }
    return result;
  }
}
