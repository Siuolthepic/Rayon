package dev.lazurite.rayon.network.tracker.generic;

import dev.lazurite.rayon.network.tracker.generic.types.BooleanType;
import dev.lazurite.rayon.network.tracker.generic.types.FloatType;
import dev.lazurite.rayon.network.tracker.generic.types.IntegerType;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

public class GenericTypeRegistry {
    public static final GenericType<Integer> INTEGER_TYPE = new IntegerType();
    public static final GenericType<Float> FLOAT_TYPE = new FloatType();
    public static final GenericType<Boolean> BOOLEAN_TYPE = new BooleanType();

    static {
        TrackedDataHandlerRegistry.register(INTEGER_TYPE);
        TrackedDataHandlerRegistry.register(FLOAT_TYPE);
        TrackedDataHandlerRegistry.register(BOOLEAN_TYPE);
    }
}