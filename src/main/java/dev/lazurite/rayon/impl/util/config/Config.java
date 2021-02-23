package dev.lazurite.rayon.impl.util.config;

import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.AnnotatedSettings;
import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.Setting;
import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.Settings;
import io.github.fablabsmc.fablabs.api.fiber.v1.exception.FiberException;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.FiberSerialization;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.JanksonValueSerializer;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigTree;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;

/**
 * The class that hosts Rayon's Fiber config.
 * @see ConfigScreen
 */
@Settings(onlyAnnotated = true)
public final class Config {
    public static final String CONFIG_NAME = "rayon.json";
    private static final Config local = new Config();
    private static Config remote;

    @Setting
    private float gravity;

    @Setting
    @Setting.Constrain.Range(min = 0.0f)
    private float airDensity;

    @Setting
    @Setting.Constrain.Range(min = 0.0f)
    private float waterDensity;

    @Setting
    @Setting.Constrain.Range(min = 0.0f)
    private float lavaDensity;

    public Config(float gravity, float airDensity, float waterDensity, float lavaDensity) {
        this.gravity = gravity;
        this.airDensity = airDensity;
        this.waterDensity = waterDensity;
        this.lavaDensity = lavaDensity;
    }

    public Config() {
        this(-9.81f, 1.2f, 997f, 3100f);
    }

    public static Config getInstance() {
        return remote == null ? local : remote;
    }

    public static void setRemote(Config remote) {
        Config.remote = remote;
    }

    public boolean isRemote() {
        return remote != null && FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }

    public void load() {
        if (Files.exists(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_NAME))) {
            try {
                FiberSerialization.deserialize(
                        ConfigTree.builder().applyFromPojo(local, AnnotatedSettings.builder().build()).build(),
                        Files.newInputStream(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_NAME)),
                        new JanksonValueSerializer(false)
                );
            } catch (IOException | FiberException e) {
                e.printStackTrace();
            }
        } else {
            this.save();
        }
    }

    public void save() {
        try {
            FiberSerialization.serialize(
                    ConfigTree.builder().applyFromPojo(local, AnnotatedSettings.builder().build()).build(),
                    Files.newOutputStream(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_NAME)),
                    new JanksonValueSerializer(false)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
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

    public float getGravity() {
        return this.gravity;
    }

    public float getAirDensity() {
        return this.airDensity;
    }

    public float  getWaterDensity() {
        return this.waterDensity;
    }

    public float getLavaDensity() {
        return this.lavaDensity;
    }
}
