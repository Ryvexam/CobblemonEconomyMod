package __PACKAGE__;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "__MOD_ID__";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Loaded {}", MOD_ID);
    }
}
