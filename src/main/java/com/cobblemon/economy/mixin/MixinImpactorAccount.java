package com.cobblemon.economy.mixin;

import com.cobblemon.economy.compat.impactor.ImpactorTransactionFactory;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;
import java.util.UUID;

@Pseudo
@Mixin(targets = "net.impactdev.impactor.core.economy.accounts.ImpactorAccount")
public abstract class MixinImpactorAccount {

    private boolean shouldUseCobecoBridge() {
        return CobblemonEconomy.getConfig() != null
                && "cobeco".equalsIgnoreCase(CobblemonEconomy.getConfig().mainCurrency);
    }

    private UUID ownerUuid() {
        try {
            Object owner = this.getClass().getMethod("owner").invoke(this);
            return owner instanceof UUID uuid ? uuid : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Inject(method = "balance", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobeco$balance(CallbackInfoReturnable<BigDecimal> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }

        UUID uuid = ownerUuid();
        if (uuid == null) {
            return;
        }

        cir.setReturnValue(CobblemonEconomy.getEconomyManager().getBalance(uuid));
    }

    @Inject(method = "set(Ljava/math/BigDecimal;)Lnet/impactdev/impactor/api/economy/transactions/EconomyTransaction;", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobeco$set(BigDecimal amount, CallbackInfoReturnable<Object> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }

        UUID uuid = ownerUuid();
        if (uuid == null || amount == null) {
            return;
        }

        CobblemonEconomy.getEconomyManager().setBalance(uuid, amount.max(BigDecimal.ZERO));
        Object tx = ImpactorTransactionFactory.buildEconomyTransaction(this, amount, "SET", "SUCCESS");
        if (tx != null) {
            cir.setReturnValue(tx);
        }
    }

    @Inject(method = "withdraw(Ljava/math/BigDecimal;)Lnet/impactdev/impactor/api/economy/transactions/EconomyTransaction;", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobeco$withdraw(BigDecimal amount, CallbackInfoReturnable<Object> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }

        UUID uuid = ownerUuid();
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }

        boolean ok = CobblemonEconomy.getEconomyManager().subtractBalance(uuid, amount);
        Object tx = ImpactorTransactionFactory.buildEconomyTransaction(
                this,
                amount,
                "WITHDRAW",
                ok ? "SUCCESS" : "NOT_ENOUGH_FUNDS"
        );
        if (tx != null) {
            cir.setReturnValue(tx);
        }
    }

    @Inject(method = "deposit(Ljava/math/BigDecimal;)Lnet/impactdev/impactor/api/economy/transactions/EconomyTransaction;", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobeco$deposit(BigDecimal amount, CallbackInfoReturnable<Object> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }

        UUID uuid = ownerUuid();
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }

        CobblemonEconomy.getEconomyManager().addBalance(uuid, amount);
        Object tx = ImpactorTransactionFactory.buildEconomyTransaction(this, amount, "DEPOSIT", "SUCCESS");
        if (tx != null) {
            cir.setReturnValue(tx);
        }
    }

    @Inject(method = "transfer(Lnet/impactdev/impactor/api/economy/accounts/Account;Ljava/math/BigDecimal;)Lnet/impactdev/impactor/api/economy/transactions/EconomyTransferTransaction;", at = @At("HEAD"), cancellable = true, require = 0)
    private void cobeco$transfer(Object to, BigDecimal amount, CallbackInfoReturnable<Object> cir) {
        if (!shouldUseCobecoBridge()) {
            return;
        }

        UUID fromUuid = ownerUuid();
        UUID toUuid = null;
        try {
            Object targetOwner = to.getClass().getMethod("owner").invoke(to);
            if (targetOwner instanceof UUID uuid) {
                toUuid = uuid;
            }
        } catch (Exception ignored) {
        }

        if (fromUuid == null || toUuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }

        boolean ok = CobblemonEconomy.getEconomyManager().subtractBalance(fromUuid, amount);
        if (ok) {
            CobblemonEconomy.getEconomyManager().addBalance(toUuid, amount);
        }

        Object tx = ImpactorTransactionFactory.buildTransferTransaction(this, to, amount, ok ? "SUCCESS" : "NOT_ENOUGH_FUNDS");
        if (tx != null) {
            cir.setReturnValue(tx);
        }
    }
}
