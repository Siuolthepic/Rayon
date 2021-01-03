package dev.lazurite.rayon.physics.entity;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import dev.lazurite.rayon.api.shape.EntityShape;
import dev.lazurite.rayon.api.shape.factory.EntityShapeFactory;
import dev.lazurite.rayon.physics.Rayon;
import dev.lazurite.rayon.physics.helper.math.QuaternionHelper;
import dev.lazurite.rayon.physics.helper.math.VectorHelper;
import dev.lazurite.rayon.physics.world.MinecraftDynamicsWorld;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.CommonTickingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class DynamicBodyEntity extends EntityRigidBody implements ComponentV3, CommonTickingComponent, AutoSyncedComponent {
    private final MinecraftDynamicsWorld dynamicsWorld;

    private DynamicBodyEntity(Entity entity, RigidBodyConstructionInfo info) {
        super(entity, info);
        this.dynamicsWorld = MinecraftDynamicsWorld.get(entity.getEntityWorld());
    }

    public static <S extends EntityShape> DynamicBodyEntity create(Entity entity, EntityShapeFactory<S> shapeFactory, float mass) {
        /* Get the entity's shape */
        CollisionShape collisionShape = (CollisionShape) shapeFactory.create(entity);

        /* Calculate the inertia of the shape. */
        Vector3f inertia = new Vector3f();
        collisionShape.calculateLocalInertia(mass, inertia);

        /* Get the position of the entity. */
        Vector3f position = VectorHelper.vec3dToVector3f(entity.getPos());

        /* Calculate the new motion state. */
        DefaultMotionState motionState = new DefaultMotionState(new Transform(new Matrix4f(new Quat4f(0, 1, 0, 0), position, 1.0f)));

        /* Create the Body based on the construction info. */
        RigidBodyConstructionInfo constructionInfo = new RigidBodyConstructionInfo(mass, motionState, collisionShape, inertia);
        DynamicBodyEntity physics = new DynamicBodyEntity(entity, constructionInfo);
        physics.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

        return physics;
    }

    public static DynamicBodyEntity get(Entity entity) {
        try {
            return Rayon.DYNAMIC_BODY_ENTITY.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void step(float delta) {
        if (!isInWorld()) {
            dynamicsWorld.addRigidBody(this);
        }
    }

    @Override
    public void tick() {
        Vector3f position = getCenterOfMassPosition(new Vector3f());
        entity.pos = VectorHelper.vector3fToVec3d(position);
        entity.updatePosition(position.x, position.y, position.z);
    }

    @Override
    public void setPosition(Vector3f position) {
        super.setPosition(position);
        entity.pos = VectorHelper.vector3fToVec3d(position);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        setOrientation(QuaternionHelper.fromTag(tag.getCompound("orientation")));
        setPosition(VectorHelper.fromTag(tag.getCompound("position")));
        setLinearVelocity(VectorHelper.fromTag(tag.getCompound("linearVelocity")));
        setAngularVelocity(VectorHelper.fromTag(tag.getCompound("angularVelocity")));
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("orientation", QuaternionHelper.toTag(getOrientation(new Quat4f())));
        tag.put("position", VectorHelper.toTag(getCenterOfMassPosition(new Vector3f())));
        tag.put("linear_velocity", VectorHelper.toTag(getLinearVelocity(new Vector3f())));
        tag.put("angular_velocity", VectorHelper.toTag(getAngularVelocity(new Vector3f())));
    }
}