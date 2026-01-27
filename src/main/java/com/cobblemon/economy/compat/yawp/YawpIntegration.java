package com.cobblemon.economy.compat.yawp;

import de.z0rdak.yawp.api.Flag;
import de.z0rdak.yawp.api.FlagEvaluator;
import de.z0rdak.yawp.api.FlagRegister;
import de.z0rdak.yawp.api.events.flag.FlagCheckRequest;
import de.z0rdak.yawp.core.flag.FlagFrequency;
import de.z0rdak.yawp.core.flag.FlagMetaInfo;
import de.z0rdak.yawp.core.flag.FlagState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public class YawpIntegration {
    public static final String FLAG_ID = "melee-npc-cobeco";
    public static Flag MELEE_NPC_FLAG;

    public static void register() {
        FlagMetaInfo meta = new FlagMetaInfo(Set.of(), FlagFrequency.NORMAL);
        FlagRegister.registerFlag("cobblemon-economy", FLAG_ID, meta);
        MELEE_NPC_FLAG = FlagRegister.getFlag(ResourceLocation.fromNamespaceAndPath("cobblemon-economy", FLAG_ID));
    }

    public static Boolean checkFlag(Entity target, Entity attacker) {
        if (MELEE_NPC_FLAG == null) return null;
        
        if (!(target.level() instanceof ServerLevel level)) return null;
        
        Player player = (attacker instanceof Player p) ? p : null;
        
        // Pass null for RegionFlag (enum), using string ID for custom flag
        FlagCheckRequest request = new FlagCheckRequest(
            target.blockPosition(), 
            null, 
            level.dimension(), 
            player,
            MELEE_NPC_FLAG.id().toString() 
        );
        
        FlagState state = FlagEvaluator.evaluate(request).getFlagState();
        if (state == FlagState.ALLOWED) return true;
        if (state == FlagState.DENIED) return false;
        return null;
    }
}
