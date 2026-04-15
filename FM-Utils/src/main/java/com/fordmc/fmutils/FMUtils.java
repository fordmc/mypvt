package com.fordmc.fmutils;

import com.fordmc.fmutils.config.FMConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FMUtils implements ModInitializer {
    public static final String MOD_ID = "fm-utils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing FM Utils...");
        FMConfig.load();
    }
}
