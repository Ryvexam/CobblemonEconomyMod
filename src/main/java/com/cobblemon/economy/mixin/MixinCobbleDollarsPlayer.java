package com.cobblemon.economy.mixin;

import com.cobblemon.economy.compat.CompatHandler;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Mixin(Player.class)
public abstract class MixinCobbleDollarsPlayer {

    private boolean shouldUseCobecoBridge() {
        return CobblemonEconomy.getConfig() != null
                && "cobeco".equalsIgnoreCase(CobblemonEconomy.getConfig().mainCurrency)
                && !CompatHandler.isCobbleDollarsBridgeBypassed()
                && (Object) this instanceof ServerPlayer;
    }

    @Dynamic
    @Inject(method = "cobbleDollars$getCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$getCobbleDollarsV2(CallbackInfoReturnable<BigInteger> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        cir.setReturnValue(BigInteger.valueOf(balance.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN).longValue()));
    }

    @Dynamic
    @Inject(method = "cobbleDollars$setCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$setCobbleDollarsV2(BigInteger amount, CallbackInfo ci) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        BigDecimal value = amount == null ? BigDecimal.ZERO : new BigDecimal(amount);
        CobblemonEconomy.getEconomyManager().setBalance(player.getUUID(), value.max(BigDecimal.ZERO));
        ci.cancel();
    }

    @Dynamic
    @Inject(method = "getCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$getCobbleDollarsV1(CallbackInfoReturnable<Integer> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        cir.setReturnValue(balance.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN).intValue());
    }

    @Dynamic
    @Inject(method = "setCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$setCobbleDollarsV1(int amount, CallbackInfo ci) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        CobblemonEconomy.getEconomyManager().setBalance(player.getUUID(), BigDecimal.valueOf(Math.max(0, amount)));
        ci.cancel();
    }

    @Dynamic
    @Inject(method = "earnCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$earnCobbleDollarsV1(int amount, CallbackInfo ci) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        if (amount > 0) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), BigDecimal.valueOf(amount));
        }
        ci.cancel();
    }

    @Dynamic
    @Inject(method = "spendCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$spendCobbleDollarsV1(int amount, CallbackInfo ci) {
        if (!shouldUseCobecoBridge()) {
            return;
        }
        if (amount > 0) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), BigDecimal.valueOf(amount));
        }
        ci.cancel();
    }
}
