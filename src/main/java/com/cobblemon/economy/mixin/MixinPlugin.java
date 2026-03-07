package com.cobblemon.economy.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mixin plugin that conditionally enables mixins based on loaded mods.
 * Add new optional mod integrations to {@link #OPTIONAL_MIXINS}.
 */
public class MixinPlugin implements IMixinConfigPlugin {

    /**
     * Map of mixin class names to their required mod IDs.
     * Mixins are only loaded if the required mod is present.
     */
    private static final Map<String, String> OPTIONAL_MIXINS = Map.of(
            "MixinCardGraderNPCEntity", "academy",  // Star Academy integration
            "MixinCobbleDollarsPlayer", "cobbledollars" // CobbleDollars bridge
            // Add more optional mixins here: "MixinClassName", "required-mod-id"
    );

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        FabricLoader loader = FabricLoader.getInstance();

        OPTIONAL_MIXINS.forEach((mixinName, requiredModId) -> {
            if (loader.isModLoaded(requiredModId)) {
                mixins.add(mixinName);
            }
        });

        return mixins;
    }

    // Required interface methods

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
