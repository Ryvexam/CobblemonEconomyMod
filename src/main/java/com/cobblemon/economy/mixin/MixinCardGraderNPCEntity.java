package com.cobblemon.economy.mixin;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Star Academy integration: charges players for card grading using Cobblemon Economy.
 * Replaces numismatic-overhaul dependency.
 */
@Pseudo
@Mixin(targets = "abeshutt.staracademy.entity.CardGraderNPCEntity", remap = false)
public abstract class MixinCardGraderNPCEntity {

    private static final ThreadLocal<Boolean> CHARGED = ThreadLocal.withInitial(() -> false);

    /** Block numismatic-overhaul check to prevent ClassNotFoundException */
    @Redirect(method = "method_5992", at = @At(value = "INVOKE", 
            target = "Ldev/architectury/platform/Platform;isModLoaded(Ljava/lang/String;)Z"))
    private boolean blockNumismaticCheck(String modId) {
        return !"numismatic-overhaul".equals(modId) && FabricLoader.getInstance().isModLoaded(modId);
    }

    /** Reset charge flag at interaction start */
    @Inject(method = "method_5992", at = @At("HEAD"))
    private void onInteractHead(Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        CHARGED.set(false);
    }

    /** Charge player before card grading */
    @Inject(method = "method_5992", at = @At(value = "INVOKE",
            target = "Labeshutt/staracademy/world/data/save/CardGradingData;add(Ljava/util/UUID;Lnet/minecraft/class_1799;)V"),
            cancellable = true)
    private void chargeForGrading(Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(user instanceof ServerPlayer player) || CHARGED.get()) return;
        CHARGED.set(true);

        long cost = getGradingCost();

        if (cost <= 0) return;

        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());

        if (balance.longValue() < cost) {
            sendBrokeMessage(player, cost, balance.longValue());
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), BigDecimal.valueOf(cost));
        player.sendSystemMessage(Component.translatable("cobblemon-economy.grading.charged", cost).withStyle(ChatFormatting.GREEN));
    }

    private void sendBrokeMessage(ServerPlayer player, long cost, long balance) {
        // Message d'erreur en rouge
        player.sendSystemMessage(Component.translatable("cobblemon-economy.grading.insufficient", balance).withStyle(ChatFormatting.RED));
        
        // Message RP en gris
        int randomIndex = new Random().nextInt(3);
        String key = "cobblemon-economy.grading.broke." + randomIndex;
        player.sendSystemMessage(Component.translatable(key, cost).withStyle(ChatFormatting.GRAY));
    }

    private static long getGradingCost() {
        try {
            Class<?> cfg = Class.forName("abeshutt.staracademy.init.ModConfigs");
            Object npc = cfg.getField("NPC").get(null);
            return ((Number) npc.getClass().getMethod("getGradingCurrencyCost").invoke(npc)).longValue();
        } catch (Exception e) {
            return 500;
        }
    }
}
