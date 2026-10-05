package Foundations.RotaryCraft.Power;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Bounded, loaded-only discovery shared by connected nodes during a server tick. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "rotarycraft")
public final class CableNetwork {
  private static final int MAX_NODES = 4096;
  private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

  private record Port(BlockPos pos, Direction side) {}

  private static final class Cache {
    long tick = Long.MIN_VALUE;
    final Map<BlockPos, Network> nodes = new HashMap<>();
  }

  private static final class Network {
    final List<Port> ports = new ArrayList<>();
    final List<PowerNodeBlockEntity> cells = new ArrayList<>();
    int remaining = 1000;
    boolean oversized;
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void unloaded(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
    if (event.getLevel() instanceof Level level) invalidate(level);
  }

  static void invalidate(Level level) {
    CACHES.remove(level);
  }

  static void distribute(Level level, PowerNodeBlockEntity source) {
    var cache = CACHES.computeIfAbsent(level, k -> new Cache());
    if (cache.tick != level.getGameTime()) {
      cache.tick = level.getGameTime();
      cache.nodes.clear();
    }
    var network = cache.nodes.get(source.getBlockPos());
    if (network == null) network = discover(level, source, cache);
    if (network.oversized || network.remaining <= 0) return;
    int budget = Math.min(network.remaining, source.transferRate());
    int size = network.ports.size();
    int start = size == 0 ? 0 : (int) Math.floorMod(level.getGameTime(), size);
    for (int i = 0; i < size && budget > 0; i++) {
      var port = network.ports.get((start + i) % size);
      if (!level.hasChunkAt(port.pos)) continue;
      var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, port.pos, port.side);
      if (target == null || !target.canReceive()) continue;
      int moved = EnergyTransfer.transfer(source.getEnergyStorage(), target, budget);
      budget -= moved;
      network.remaining -= moved;
    }
    // Cells hold surplus; stored cell power never circulates through cable buffers.
    for (var cell : network.cells) {
      if (budget <= 0) break;
      if (cell == source || cell.isRemoved() || !level.hasChunkAt(cell.getBlockPos())) continue;
      int limit = budget;
      if (source.isCell()) {
        var from = source.getEnergyStorage();
        var to = cell.getEnergyStorage();
        long excess =
            (long) from.getEnergyStored() * to.getMaxEnergyStored()
                - (long) to.getEnergyStored() * from.getMaxEnergyStored();
        limit =
            (int)
                Math.min(
                    budget,
                    Math.max(0, excess / (from.getMaxEnergyStored() + to.getMaxEnergyStored())));
      }
      int moved =
          EnergyTransfer.transfer(source.getEnergyStorage(), cell.getEnergyStorage(), limit);
      budget -= moved;
      network.remaining -= moved;
    }
  }

  private static Network discover(Level level, PowerNodeBlockEntity source, Cache cache) {
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
      if (entity instanceof PowerNodeBlockEntity node) {
        result.remaining = Math.min(result.remaining, node.transferRate());
        if (node.isCell()) result.cells.add(node);
      }
      for (var direction : Direction.values()) {
        var next = pos.relative(direction);
        if (!level.hasChunkAt(next)) continue;
        var neighbor = level.getBlockEntity(next);
        boolean wire =
            neighbor instanceof PowerNodeBlockEntity
                || neighbor instanceof PowerSwitchBlockEntity gate && gate.isEnabled();
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
    for (var pos : seen) cache.nodes.put(pos, result);
    return result;
  }
}
