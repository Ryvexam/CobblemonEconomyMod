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

    /**
     * Returns true when we should intercept CobbleDollars methods.
     * Active when mainCurrency is "cobeco" or "impactor" — in both cases,
     * CobbleDollars is NOT the source of truth and should delegate elsewhere.
     * When mainCurrency is "cobbledollars", CobbleDollars IS the master so we don't intercept.
     */
    private boolean shouldInterceptCobbleDollars() {
        if (CobblemonEconomy.getConfig() == null) return false;
        if (CompatHandler.isCobbleDollarsBridgeBypassed()) return false;
        if (!((Object) this instanceof ServerPlayer)) return false;
        String main = CobblemonEconomy.getConfig().mainCurrency;
        // Intercept when cobeco (our DB is master) or impactor (Impactor is master)
        // Don't intercept when cobbledollars (CobbleDollars is master)
        return !"cobbledollars".equalsIgnoreCase(main);
    }

    @Dynamic
    @Inject(method = "cobbleDollars$getCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$getCobbleDollarsV2(CallbackInfoReturnable<BigInteger> cir) {
        if (!shouldInterceptCobbleDollars()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        cir.setReturnValue(BigInteger.valueOf(balance.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN).longValue()));
    }

    @Dynamic
    @Inject(method = "cobbleDollars$setCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$setCobbleDollarsV2(BigInteger amount, CallbackInfo ci) {
        if (!shouldInterceptCobbleDollars()) {
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
        if (!shouldInterceptCobbleDollars()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        BigDecimal balance = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        cir.setReturnValue(balance.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN).intValue());
    }

    @Dynamic
    @Inject(method = "setCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$setCobbleDollarsV1(int amount, CallbackInfo ci) {
        if (!shouldInterceptCobbleDollars()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        CobblemonEconomy.getEconomyManager().setBalance(player.getUUID(), BigDecimal.valueOf(Math.max(0, amount)));
        ci.cancel();
    }

    @Dynamic
    @Inject(method = "earnCobbleDollars", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobeco$earnCobbleDollarsV1(int amount, CallbackInfo ci) {
        if (!shouldInterceptCobbleDollars()) {
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
        if (!shouldInterceptCobbleDollars()) {
            return;
        }
        if (amount > 0) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), BigDecimal.valueOf(amount));
        }
        ci.cancel();
    }
}
