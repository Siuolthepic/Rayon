package dev.lazurite.rayon.physics.helper;

import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lazurite.rayon.physics.world.MinecraftDynamicsWorld;
import dev.lazurite.rayon.physics.util.Constants;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.*;

public class BlockHelper {
    private final MinecraftDynamicsWorld dynamicsWorld;
    private final JsonArray blockProperties;
    private final Map<BlockPos, RigidBody> collisionBlocks;

    public BlockHelper(MinecraftDynamicsWorld dynamicsWorld)  {
        this.dynamicsWorld = dynamicsWorld;
        this.blockProperties = PropertyHelper.get("blocks");
        this.collisionBlocks = Maps.newHashMap();
    }

    public void load(EntityHelper entities) {
        entities.getEntities().forEach(this::load);
    }

    public void load(Entity entity) {
        World world = dynamicsWorld.getWorld();
        Box area = new Box(new BlockPos(entity.getPos())).expand(Constants.BLOCK_RADIUS);
        Map<BlockPos, BlockState> blockList = getBlockList(world, area);
        BlockView blockView = world.getChunkManager().getChunk(entity.chunkX, entity.chunkZ);
        List<BlockPos> toKeepBlocks = Lists.newArrayList();

        blockList.forEach((blockPos, blockState) -> {
            float friction = 1.0f;
            boolean permeable = false;

            /* Get properties for this specific block */
            for (JsonElement property : blockProperties) {
                String currentBlock = blockState.getBlock().getTranslationKey();
                String name = ((JsonObject) property).get("name").toString();

                if (currentBlock.equals(name)) {
                    friction = ((JsonObject) property).get("friction").getAsFloat();
                    permeable = ((JsonObject) property).get("permeable").getAsBoolean();
                }
            }

            /* Check if block is solid or not */
            if (!blockState.getBlock().canMobSpawnInside() && !permeable) {
                if (!this.collisionBlocks.containsKey(blockPos)) {
                    VoxelShape coll = blockState.getCollisionShape(blockView, blockPos);

                    if (!coll.isEmpty()) {
                        /* Create the box shape for the block */
                        Box b = coll.getBoundingBox();
                        Vector3f box = new Vector3f(
                                ((float) (b.maxX - b.minX) / 2.0F) + 0.005f,
                                ((float) (b.maxY - b.minY) / 2.0F) + 0.005f,
                                ((float) (b.maxZ - b.minZ) / 2.0F) + 0.005f);
                        CollisionShape shape = new BoxShape(box);

                        /* Set the position of the rigid body to the block's position */
                        Vector3f position = new Vector3f(blockPos.getX() + box.x, blockPos.getY() + box.y, blockPos.getZ() + box.z);
                        DefaultMotionState motionState = new DefaultMotionState(new Transform(new Matrix4f(new Quat4f(), position, friction)));

                        /* Set up the rigid body's construction info and initialization */
                        RigidBodyConstructionInfo ci = new RigidBodyConstructionInfo(0, motionState, shape, new Vector3f(0, 0, 0));
                        RigidBody body = new RigidBody(ci);

                        /* Add it to the necessary locations */
                        collisionBlocks.put(blockPos, body);
                        this.dynamicsWorld.addRigidBody(body);
                    }
                }

                toKeepBlocks.add(blockPos);
            }
        });

        // TODO clean this up
        List<BlockPos> toRemove = Lists.newArrayList();
        this.collisionBlocks.forEach((pos, body) -> {
            if (!toKeepBlocks.contains(pos)) {
                dynamicsWorld.removeRigidBody(body);
                toRemove.add(pos);
            }
        });
        toRemove.forEach(this.collisionBlocks::remove);
    }

    public boolean contains(RigidBody body) {
        return this.collisionBlocks.containsValue(body);
    }

    public Collection<RigidBody> getRigidBodies() {
        return this.collisionBlocks.values();
    }

    /*/**
     * Finds and returns a {@link Set} of {@link Block} objects that the
     * {@link Entity} is touching based on the provided {@link Direction}(s)
     * @param directions the {@link Direction}s of the desired touching {@link Block}s
     * @return a list of touching {@link Block}s
     */
    /*public static Set<Block> getTouchingBlocks(Entity entity, Direction... directions) {
        PhysicsWorld physicsWorld = PhysicsWorld.INSTANCE;
//        DynamicBodyComposition physics = ((DynamicBody) entity).getDynamicBody();

        Dispatcher dispatcher = physicsWorld.getDispatcher();
        Set<Block> blocks = Sets.newHashSet();

        for (int manifoldNum = 0; manifoldNum < dispatcher.getNumManifolds(); ++manifoldNum) {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(manifoldNum);

            if (physicsWorld.blockHelper.contains((RigidBody) manifold.getBody0()) &&
                    physicsWorld.getBlockHelper().contains((RigidBody) manifold.getBody1())) {
                continue;
            }

            for (int contactNum = 0; contactNum < manifold.getNumContacts(); ++contactNum) {
                if (manifold.getContactPoint(contactNum).getDistance() <= 0.0f) {
//                    if (physics.getRigidBody().equals(manifold.getBody0()) || physics.getRigidBody().equals(manifold.getBody1())) {
//                        Vector3f droneRigidBodyPos = physics.getRigidBody().equals(manifold.getBody0()) ? ((RigidBody) manifold.getBody0()).getCenterOfMassPosition(new Vector3f()) : ((RigidBody) manifold.getBody1()).getCenterOfMassPosition(new Vector3f());
//                        Vector3f otherRigidBodyPos = physics.getRigidBody().equals(manifold.getBody0()) ? ((RigidBody) manifold.getBody1()).getCenterOfMassPosition(new Vector3f()) : ((RigidBody) manifold.getBody0()).getCenterOfMassPosition(new Vector3f());
//
//                        for (Direction direction : directions) {
//                            switch (direction) {
//                                case UP:
//                                    if (droneRigidBodyPos.y < otherRigidBodyPos.y) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                case DOWN:
//                                    if (droneRigidBodyPos.y > otherRigidBodyPos.y) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                case EAST:
//                                    if (droneRigidBodyPos.x < otherRigidBodyPos.x) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                case WEST:
//                                    if (droneRigidBodyPos.x > otherRigidBodyPos.x) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                case NORTH:
//                                    if (droneRigidBodyPos.z < otherRigidBodyPos.z) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                case SOUTH:
//                                    if (droneRigidBodyPos.z > otherRigidBodyPos.z) {
//                                        blocks.add(entity.world.getBlockState(new BlockPos(otherRigidBodyPos.x, otherRigidBodyPos.y, otherRigidBodyPos.z)).getBlock());
//                                    }
//                                    break;
//                                default:
//                                    break;
//                            }
//                        }
//                    }
                }
            }
        }
        return blocks;
    } */

    public static Map<BlockPos, BlockState> getBlockList(World world, Box area) {
        Map<BlockPos, BlockState> map = Maps.newHashMap();
        for (int i = (int) area.minX; i < area.maxX; i++) {
            for (int j = (int) area.minY; j < area.maxY; j++) {
                for (int k = (int) area.minZ; k < area.maxZ; k++) {
                    BlockPos blockPos = new BlockPos(i, j, k);
                    BlockState blockState = world.getWorldChunk(blockPos).getBlockState(blockPos);
                    map.put(blockPos, blockState);
                }
            }
        }

        return map;
    }
}