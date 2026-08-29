package com.yungnickyoung.minecraft.ribbits.client.screen;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.config.RibbitsConfig;
import com.yungnickyoung.minecraft.ribbits.util.GeoIP;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RibbitsClothScreen {
    private static final String TRANSLATION_PREFIX =
            "text.autoconfig.ribbits-" + RibbitsCommon.MC_VERSION_STRING;

    private static Component translation(String suffix) {
        return Component.translatable(TRANSLATION_PREFIX + suffix);
    }

    public static Screen create(Screen parent) {
        RibbitsConfig config = AutoConfig.getConfigHolder(RibbitsConfig.class).getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(translation(".title"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        var general = builder.getOrCreateCategory(
                translation(".option.general")
        );

        general.addEntry(
                eb.startBooleanToggle(
                                translation(".option.general.prideFlagAllYear"),
                                config.general.prideFlagAllYear
                        )
                        .setDefaultValue(false)
                        .setTooltip(translation(".option.general.prideFlagAllYear.@Tooltip"))
                        .setSaveConsumer(val -> config.general.prideFlagAllYear = val)
                        .build()
        );

        if (GeoIP.isInChina()) {
            general.addEntry(
                    eb.startBooleanToggle(
                                    translation(".option.general.disablePrideFlagCN"),
                                    config.general.disablePrideFlagCN
                            )
                            .setDefaultValue(false)
                            .setTooltip(translation(".option.general.disablePrideFlagCN.@Tooltip"))
                            .setSaveConsumer(val -> config.general.disablePrideFlagCN = val)
                            .build()
            );
        }

        var network = builder.getOrCreateCategory(
                translation(".option.network")
        );

        network.addEntry(
                eb.startStrField(
                                translation(".option.network.proxyHost"),
                                config.network.proxyHost
                        ).setTooltip(translation(".option.network.proxyHost.@Tooltip"))
                        .setSaveConsumer(val -> config.network.proxyHost = val)
                        .build()
        );

        network.addEntry(
                eb.startIntField(
                                translation(".option.network.proxyPort"),
                                config.network.proxyPort
                        ).setTooltip(translation(".option.network.proxyPort.@Tooltip"))
                        .setSaveConsumer(val -> config.network.proxyPort = val)
                        .build()
        );

        network.addEntry(
                eb.startStrField(
                                translation(".option.network.proxyUsername"),
                                config.network.proxyUsername
                        ).setTooltip(translation(".option.network.proxyUsername.@Tooltip"))
                        .setSaveConsumer(val -> config.network.proxyUsername = val)
                        .build()
        );

        network.addEntry(
                eb.startStrField(
                                translation(".option.network.proxyPassword"),
                                config.network.proxyPassword
                        ).setTooltip(translation(".option.network.proxyPassword.@Tooltip"))
                        .setSaveConsumer(val -> config.network.proxyPassword = val)
                        .build()
        );

        builder.setSavingRunnable(() -> AutoConfig.getConfigHolder(RibbitsConfig.class).save());

        return builder.build();
    }
}
