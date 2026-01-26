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
import java.util.function.Function;

/**
 * Star Academy integration: charges players for card grading using Cobblemon Economy.
 * Replaces numismatic-overhaul dependency.
 */
@Pseudo
@Mixin(targets = "abeshutt.staracademy.entity.CardGraderNPCEntity", remap = false)
public abstract class MixinCardGraderNPCEntity {

    @Shadow @Final public static Function<Object, String> INITIAL_BROKE;

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
        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());

        if (balance.longValue() < cost) {
            sendBrokeMessage(player);
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), BigDecimal.valueOf(cost));
        player.sendSystemMessage(Component.literal("Charged " + cost + " for card grading!").withStyle(ChatFormatting.GREEN));
    }

    private void sendBrokeMessage(ServerPlayer player) {
        try {
            Object randomProxy = java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Class.forName("abeshutt.staracademy.math.random.RandomSource")},
                    (p, m, a) -> m.getName().equals("nextInt") ? new Random().nextInt((int) a[0]) : null
            );
            String key = (String) INITIAL_BROKE.apply(randomProxy);
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("You can't afford this!").withStyle(ChatFormatting.RED));
        }
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
