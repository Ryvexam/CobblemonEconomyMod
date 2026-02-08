package com.cobblemon.economy.mixin;

import com.cobblemon.economy.compat.CompatHandler;
import com.cobblemon.economy.compat.impactor.ImpactorAccountBridge;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets = "net.impactdev.impactor.core.economy.ImpactorEconomyService", remap = false)
public abstract class MixinImpactorEconomyService {

    @Dynamic
    @Inject(method = "account(Lnet/impactdev/impactor/api/economy/currency/Currency;Ljava/util/UUID;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"), cancellable = true, remap = false)
    private void cobeco$wrapAccountFuture(@Coerce Object currency, @Coerce Object uuid, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        wrap(cir);
    }

    @Dynamic
    @Inject(method = "account(Lnet/impactdev/impactor/api/economy/currency/Currency;Ljava/util/UUID;Lnet/impactdev/impactor/api/economy/accounts/Account$AccountModifier;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"), cancellable = true, remap = false)
    private void cobeco$wrapAccountFutureWithModifier(@Coerce Object currency, @Coerce Object uuid, @Coerce Object modifier, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        wrap(cir);
    }

    private void wrap(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        CompletableFuture<?> future = cir.getReturnValue();
        if (future == null || CompatHandler.isImpactorBridgeBypassed()) {
            return;
        }

        cir.setReturnValue(future.thenApply(ImpactorAccountBridge::wrapIfNeeded));
    }
}
