package com.cobblemon.economy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.Blocks;

public class QuestBoardBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public QuestBoardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.ORIGIN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos origin = context.getClickedPos();
        if (!canPlaceAt(context.getLevel(), origin, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, Part.ORIGIN);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.getValue(FACING);

        for (Part part : Part.values()) {
            BlockPos target = partPos(pos, facing, part);
            if (!target.equals(pos) && !level.getBlockState(target).canBeReplaced()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
                return;
            }
        }

        for (Part part : Part.values()) {
            if (part == Part.ORIGIN) {
                continue;
            }
            BlockPos target = partPos(pos, facing, part);
            level.setBlock(target, this.defaultBlockState().setValue(FACING, facing).setValue(PART, part), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() == newState.getBlock()) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }

        if (!level.isClientSide) {
            BlockPos origin = findOrigin(level, pos, state);
            Direction facing = state.getValue(FACING);
            for (Part part : Part.values()) {
                BlockPos target = partPos(origin, facing, part);
                if (target.equals(pos)) {
                    continue;
                }
                BlockState bs = level.getBlockState(target);
                if (bs.getBlock() == this) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static BlockPos findOrigin(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof QuestBoardBlock)) {
            return pos;
        }
        Direction facing = state.getValue(FACING);
        Part part = state.getValue(PART);
        Direction right = facing.getClockWise();
        return pos.relative(right.getOpposite(), part.x).below(part.y);
    }

    private static BlockPos partPos(BlockPos origin, Direction facing, Part part) {
        Direction right = facing.getClockWise();
        return origin.relative(right, part.x).above(part.y);
    }

    private static boolean canPlaceAt(Level level, BlockPos origin, Direction facing) {
        for (Part part : Part.values()) {
            BlockPos target = partPos(origin, facing, part);
            if (!level.getBlockState(target).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public enum Part implements StringRepresentable {
        ORIGIN(0, 0, "origin"),
        LEFT(-1, 0, "left"),
        RIGHT(1, 0, "right"),
        TOP_LEFT(-1, 1, "top_left"),
        TOP(0, 1, "top"),
        TOP_RIGHT(1, 1, "top_right");

        public final int x;
        public final int y;
        private final String id;

        Part(int x, int y, String id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
