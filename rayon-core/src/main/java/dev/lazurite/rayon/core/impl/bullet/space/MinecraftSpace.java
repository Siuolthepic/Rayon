package dev.lazurite.rayon.core.impl.bullet.space;

import com.google.common.collect.Lists;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import dev.lazurite.rayon.core.api.element.PhysicsElement;
import dev.lazurite.rayon.core.api.event.ElementCollisionEvents;
import dev.lazurite.rayon.core.api.event.PhysicsSpaceEvents;
import dev.lazurite.rayon.core.impl.bullet.body.BlockRigidBody;
import dev.lazurite.rayon.core.impl.bullet.body.ElementRigidBody;
import dev.lazurite.rayon.core.impl.bullet.thread.PhysicsThread;
import dev.lazurite.rayon.core.impl.util.space.SpaceStorage;
import dev.lazurite.rayon.core.impl.util.thread.Clock;
import dev.lazurite.rayon.core.impl.util.thread.Pausable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * This is the physics simulation environment for all {@link BlockRigidBody}s and {@link ElementRigidBody}s. It runs
 * on a separate thread from the rest of the game using {@link PhysicsThread}. Users shouldn't have to interact with
 * this object too much.<br>
 * To gain access to the world's {@link MinecraftSpace}, you can call {@link MinecraftSpace#get(World)} or register
 * a step event in {@link PhysicsSpaceEvents}. As a rule of thumb, if you need to modify information in the physics
 * environment (e.g. add a rigid body or apply a force) then you should always perform those operations on the same
 * thread. The easiest way to get onto the physics thread is to queue a task using {@link PhysicsThread#execute(Runnable)}.
 * The {@link PhysicsThread} can be accessed using {@link MinecraftSpace#getThread()}. If you only need to read information
 * from the physics world, you don't need to queue a task on the physics thread.
 * @see PhysicsThread
 * @see PhysicsSpaceEvents
 */
public class MinecraftSpace extends PhysicsSpace implements Pausable, PhysicsCollisionListener {
    private static final int MAX_PRESIM_STEPS = 30;

    private final TerrainManager terrainManager;
    private final PhysicsThread thread;
    private final World world;
    private final Clock clock;
    private int presimSteps;

    private float airDensity;
    private float waterDensity;
    private float lavaDensity;

    /**
     * Allows users to retrieve the {@link MinecraftSpace} associated
     * with any given {@link World} object (client or server).
     * @param world the world to get the physics space from
     * @return the {@link MinecraftSpace}
     */
    public static MinecraftSpace get(World world) {
        return ((SpaceStorage) world).getSpace();
    }

    public MinecraftSpace(PhysicsThread thread, World world, BroadphaseType broadphase) {
        super(broadphase);
        this.thread = thread;
        this.world = world;
        this.clock = new Clock();
        this.terrainManager = new TerrainManager(this);
        this.addCollisionListener(this);

        this.setGravity(new Vector3f(0, -9.807f, 0)); // m/s/s
        this.setAirDensity(1.2f); // kg/m^3
        this.setWaterDensity(997f); // kg/m^3
        this.setLavaDensity(3100f); // kg/m^3
    }

    public MinecraftSpace(PhysicsThread thread, World world) {
        this(thread, world, BroadphaseType.DBVT);
    }

    /**
     * This method performs the following steps:
     * <ul>
     *     <li>Fires world step events in {@link PhysicsSpaceEvents}.</li>
     *     <li>Steps {@link ElementRigidBody}s.</li>
     *     <li>Applies air drag force to all {@link ElementRigidBody}s.</li>
     *     <li>Loads blocks into the simulation around {@link ElementRigidBody}s using {@link TerrainManager}.</li>
     *     <li>Triggers all collision events (queues up tasks in server thread).</li>
     *     <li>Steps the simulation using {@link PhysicsSpace#update(float, int)}.</li>
     * </ul>
     *
     * Additionally, none of the above steps execute when either the world is empty
     * (no {@link PhysicsRigidBody}s) or when the game is paused.
     *
     * @see TerrainManager
     * @see PhysicsSpaceEvents
     */
    public void step() {
        if (!isPaused() && (!isEmpty() || isInPresim())) {
            float delta = this.clock.get();

            /* World Step Event */
            PhysicsSpaceEvents.STEP.invoker().onStep(this);

            /* Step and Fluid Resistance */
            getRigidBodiesByClass(ElementRigidBody.class).forEach(body -> {
                body.getElement().step(this);

                if (body.shouldDoFluidResistance()) {
                    body.applyDrag();
                }

                /* Environment Loading */
                if (!body.isInNoClip()) {
                    Vector3f pos = body.getPhysicsLocation(new Vector3f());
                    Box box = new Box(new BlockPos(pos.x, pos.y, pos.z)).expand(body.getEnvironmentLoadDistance());

                    getTerrainManager().load(body, box);
//                    getEntityManager().load(box);
                }
            });

            getTerrainManager().purge();
//            getEntityManager().purge();

            /* Collision Events */
            if (!getWorld().isClient()) {
                distributeEvents();
            }

            /* Step Simulation */
            if (presimSteps > MAX_PRESIM_STEPS) {
                update(delta, 5);
            } else ++presimSteps;
        } else {
            this.clock.reset();
        }
    }

    public void load(PhysicsElement element) {
        ElementRigidBody rigidBody = element.getRigidBody();

        if (!rigidBody.isInWorld()) {
            element.reset();
            addCollisionObject(rigidBody);
        }

        rigidBody.activate();
    }

    public void unload(PhysicsElement element) {
        if (element.getRigidBody().isInWorld()) {
            removeCollisionObject(element.getRigidBody());
        }
    }

    public TerrainManager getTerrainManager() {
        return this.terrainManager;
    }

    public boolean isInPresim() {
        return presimSteps < MAX_PRESIM_STEPS;
    }

    public <T> List<T> getRigidBodiesByClass(Class<T> type) {
        List<T> out = Lists.newArrayList();

        for (PhysicsRigidBody body : getRigidBodyList()) {
            if (type.isAssignableFrom(body.getClass())) {
                out.add(type.cast(body));
            }
        }

        return out;
    }

    public PhysicsThread getThread() {
        return this.thread;
    }

    public World getWorld() {
        return this.world;
    }

    public void setAirDensity(float airDensity) {
        this.airDensity = airDensity;
    }

    public void setWaterDensity(float waterDensity) {
        this.waterDensity = waterDensity;
    }

    public void setLavaDensity(float lavaDensity) {
        this.lavaDensity = lavaDensity;
    }

    public float getAirDensity() {
        return this.airDensity;
    }

    public float getWaterDensity() {
        return this.waterDensity;
    }

    public float getLavaDensity() {
        return this.lavaDensity;
    }

    /**
     * Trigger all collision events (e.g. block/element or element/element).
     * Executed on the main thread rather than the physics thread.
     * @param event the event context
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {
        getThread().getThreadExecutor().execute(() -> {
            if (event.getObjectA() instanceof ElementRigidBody && event.getObjectB() instanceof ElementRigidBody) {
                PhysicsElement element1 = ((ElementRigidBody) event.getObjectA()).getElement();
                PhysicsElement element2 = ((ElementRigidBody) event.getObjectB()).getElement();
                ElementCollisionEvents.ELEMENT_COLLISION.invoker().onCollide(element1, element2);

            } else if (event.getObjectA() instanceof BlockRigidBody && event.getObjectB() instanceof ElementRigidBody) {
                BlockPos blockPos = ((BlockRigidBody) event.getObjectA()).getBlockPos();
                BlockState blockState = ((BlockRigidBody) event.getObjectA()).getBlockState();
                PhysicsElement element = ((ElementRigidBody) event.getObjectB()).getElement();
                ElementCollisionEvents.BLOCK_COLLISION.invoker().onCollide(element, blockPos, blockState);

            } else if (event.getObjectA() instanceof ElementRigidBody && event.getObjectB() instanceof BlockRigidBody) {
                BlockPos blockPos = ((BlockRigidBody) event.getObjectB()).getBlockPos();
                BlockState blockState = ((BlockRigidBody) event.getObjectB()).getBlockState();
                PhysicsElement element = ((ElementRigidBody) event.getObjectA()).getElement();
                ElementCollisionEvents.BLOCK_COLLISION.invoker().onCollide(element, blockPos, blockState);
            }
        });
    }
}