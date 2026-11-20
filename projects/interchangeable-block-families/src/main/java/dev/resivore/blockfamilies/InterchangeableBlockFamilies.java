package dev.resivore.blockfamilies;

import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InterchangeableBlockFamilies implements ModInitializer {
    public static final String MOD_ID = "interchangeable_block_families";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("IBF catalog declares {} CNM ShapeMap families across supported providers ({} distinct item IDs, largest family {})",
                AuditedShapeFamilies.families().size(),
                AuditedShapeFamilies.uniqueMemberCount(),
                AuditedShapeFamilies.largestFamilySize());
    }
}
