package Foundations.RotaryCraft.Geometry;
public final class MachineShapes {
    private static final java.util.Map<net.minecraft.world.level.block.state.BlockState,net.minecraft.world.phys.shapes.VoxelShape> CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    public static net.minecraft.world.phys.shapes.VoxelShape shape(net.minecraft.world.level.block.state.BlockState state) {
        return CACHE.computeIfAbsent(state,MachineShapes::build);
    }
    private static net.minecraft.world.phys.shapes.VoxelShape build(net.minecraft.world.level.block.state.BlockState state) {
        var shape = switch(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()) {
            case "solar_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(3,0,3,13,7,13),net.minecraft.world.level.block.Block.box(0,7,0,16,9,16));
            case "power_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "steam_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "geothermal_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "hydro_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "wind_generator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "shaft" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,13,16));
            case "dc_engine" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "power_cell" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(1,0,1,15,16,15));
            case "winder" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,6,16));
            case "mob_harvester" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,16,16));
            case "defoliator" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,9,16));
            case "player_detector" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(2,0,2,14,12,14));
            case "smoke_detector" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(2,0,2,14,1,14));
            case "sprinkler" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(3,0,3,13,9,13));
            case "gearbox_2" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,13,16));
            case "gearbox_4" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,13,16));
            case "gearbox_8" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,13,16));
            case "gearbox_16" -> net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.level.block.Block.box(0,0,0,16,13,16));
            default -> net.minecraft.world.phys.shapes.Shapes.block();
        };
        var facing=state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)?state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING):state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)?state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING):net.minecraft.core.Direction.NORTH;
        var output=net.minecraft.world.phys.shapes.Shapes.empty();
        for(var box:shape.toAabbs()) {
            double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
            for(double x:new double[]{box.minX-.5,box.maxX-.5})for(double y:new double[]{box.minY-.5,box.maxY-.5})for(double z:new double[]{box.minZ-.5,box.maxZ-.5}) {
                double a=x,b=y,c=z;
                switch(facing){case EAST->{a=-z;c=x;}case SOUTH->{a=-x;c=-z;}case WEST->{a=z;c=-x;}case UP->{b=-z;c=y;}case DOWN->{b=z;c=-y;}default->{}}
                minX=Math.min(minX,a+.5);minY=Math.min(minY,b+.5);minZ=Math.min(minZ,c+.5);maxX=Math.max(maxX,a+.5);maxY=Math.max(maxY,b+.5);maxZ=Math.max(maxZ,c+.5);
            }
            output=net.minecraft.world.phys.shapes.Shapes.or(output,net.minecraft.world.phys.shapes.Shapes.box(minX,minY,minZ,maxX,maxY,maxZ));
        }
        return output;
    }
}
