package dev.lazurite.rayon.impl.util.config;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.TranslatableText;

/**
 * Handles creation of the config screen and also
 * the process of opening it within mod menu.
 */
@Environment(EnvType.CLIENT)
public class ConfigScreen implements ModMenuApi {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(new TranslatableText("config.rayon.title"))
                .setTransparentBackground(true)
                .setSavingRunnable(() -> {
                    Config.getInstance().save();

                    MinecraftClient client = MinecraftClient.getInstance();
                    IntegratedServer server = client.getServer();

                    if (server != null) {
                        server.getPlayerManager().getPlayerList().forEach(player -> {
                            if (!player.equals(client.player)) {
                                ConfigS2C.send(player, Config.getInstance());
                            }
                        });
                    }
                });

        return builder.setFallbackCategory(getPhysicsSettings(builder)).build();
    }

    public static ConfigCategory getPhysicsSettings(ConfigBuilder builder) {
        ConfigCategory category = builder.getOrCreateCategory(new TranslatableText("config.rayon.title"));

        if (!Config.getInstance().isRemote()) {
            /* Air Density */
            category.addEntry(builder.entryBuilder().startFloatField(
                    new TranslatableText("config.rayon.option.fluid.air.density"), Config.getInstance().getAirDensity())
                    .setDefaultValue(1.2f)
                    .setTooltip(
                            new TranslatableText("config.rayon.option.fluid.air.density.tooltip"),
                            new TranslatableText("config.rayon.option.performance.low"))
                    .setSaveConsumer(newValue -> Config.getInstance().setAirDensity(newValue))
                    .build());

            /* Water Density */
            category.addEntry(builder.entryBuilder().startFloatField(
                    new TranslatableText("config.rayon.option.fluid.water.density"), Config.getInstance().getWaterDensity())
                    .setDefaultValue(997f)
                    .setTooltip(
                            new TranslatableText("config.rayon.option.fluid.water.density.tooltip"),
                            new TranslatableText("config.rayon.option.performance.low"))
                    .setSaveConsumer(newValue -> Config.getInstance().setWaterDensity(newValue))
                    .build());

            /* Lava Density */
            category.addEntry(builder.entryBuilder().startFloatField(
                    new TranslatableText("config.rayon.option.fluid.lava.density"), Config.getInstance().getLavaDensity())
                    .setDefaultValue(3100f)
                    .setTooltip(
                            new TranslatableText("config.rayon.option.fluid.lava.density.tooltip"),
                            new TranslatableText("config.rayon.option.performance.low"))
                    .setSaveConsumer(newValue -> Config.getInstance().setLavaDensity(newValue))
                    .build());

            /* Gravity */
            category.addEntry(builder.entryBuilder().startFloatField(
                    new TranslatableText("config.rayon.option.gravity"), Config.getInstance().getGravity())
                    .setDefaultValue(-9.81f)
                    .setTooltip(
                            new TranslatableText("config.rayon.option.gravity.tooltip"),
                            new TranslatableText("config.rayon.option.performance.low"))
                    .setSaveConsumer(newValue -> Config.getInstance().setGravity(newValue))
                    .build());
        }

        return category;
    }

    /**
     * Adds the config screen to mod menu.
     * @return the {@link ConfigScreenFactory}
     */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
}
