package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.compat.CompatHandler;
import com.cobblemon.economy.fabric.CobblemonEconomy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ImpactorAccountBridge {
    private ImpactorAccountBridge() {
    }

    public static Object wrapIfNeeded(Object account) {
        if (account == null || CompatHandler.isImpactorBridgeBypassed()) {
            return account;
        }
        if (CobblemonEconomy.getConfig() == null || !"cobeco".equalsIgnoreCase(CobblemonEconomy.getConfig().mainCurrency)) {
            return account;
        }

        try {
            Class<?> accountInterface = Class.forName("net.impactdev.impactor.api.economy.accounts.Account");
            if (!accountInterface.isInstance(account)) {
                return account;
            }

            if (Proxy.isProxyClass(account.getClass())) {
                return account;
            }

            return Proxy.newProxyInstance(
                    account.getClass().getClassLoader(),
                    new Class<?>[]{accountInterface},
                    (proxy, method, args) -> invoke(account, method, args)
            );
        } catch (Exception ignored) {
            return account;
        }
    }

    private static Object invoke(Object account, Method method, Object[] args) throws Exception {
        String name = method.getName();

        if ("balance".equals(name)) {
            UUID owner = owner(account);
            return owner == null ? method.invoke(account, args) : CobblemonEconomy.getEconomyManager().getBalance(owner);
        }

        if ("set".equals(name) && args != null && args.length == 1 && args[0] instanceof BigDecimal amount) {
            UUID owner = owner(account);
            if (owner == null) {
                return method.invoke(account, args);
            }
            BigDecimal target = amount.max(BigDecimal.ZERO);
            CobblemonEconomy.getEconomyManager().setBalance(owner, target);
            Object tx = ImpactorTransactionFactory.buildEconomyTransaction(account, amount, "SET", "SUCCESS");
            return tx != null ? tx : method.invoke(account, args);
        }

        if ("withdraw".equals(name) && args != null && args.length == 1 && args[0] instanceof BigDecimal amount) {
            UUID owner = owner(account);
            if (owner == null) {
                return method.invoke(account, args);
            }
            boolean ok = CobblemonEconomy.getEconomyManager().subtractBalance(owner, amount.max(BigDecimal.ZERO));
            Object tx = ImpactorTransactionFactory.buildEconomyTransaction(account, amount, "WITHDRAW", ok ? "SUCCESS" : "NOT_ENOUGH_FUNDS");
            return tx != null ? tx : method.invoke(account, args);
        }

        if ("deposit".equals(name) && args != null && args.length == 1 && args[0] instanceof BigDecimal amount) {
            UUID owner = owner(account);
            if (owner == null) {
                return method.invoke(account, args);
            }
            CobblemonEconomy.getEconomyManager().addBalance(owner, amount.max(BigDecimal.ZERO));
            Object tx = ImpactorTransactionFactory.buildEconomyTransaction(account, amount, "DEPOSIT", "SUCCESS");
            return tx != null ? tx : method.invoke(account, args);
        }

        if ("transfer".equals(name) && args != null && args.length == 2 && args[1] instanceof BigDecimal amount) {
            UUID from = owner(account);
            UUID to = owner(args[0]);
            if (from == null || to == null) {
                return method.invoke(account, args);
            }
            boolean ok = CobblemonEconomy.getEconomyManager().subtractBalance(from, amount.max(BigDecimal.ZERO));
            if (ok) {
                CobblemonEconomy.getEconomyManager().addBalance(to, amount.max(BigDecimal.ZERO));
            }
            Object tx = ImpactorTransactionFactory.buildTransferTransaction(account, args[0], amount, ok ? "SUCCESS" : "NOT_ENOUGH_FUNDS");
            return tx != null ? tx : method.invoke(account, args);
        }

        if ("reset".equals(name)) {
            UUID owner = owner(account);
            if (owner == null) {
                return method.invoke(account, args);
            }
            CobblemonEconomy.getEconomyManager().setBalance(owner, BigDecimal.ZERO);
            Object tx = ImpactorTransactionFactory.buildEconomyTransaction(account, BigDecimal.ZERO, "RESET", "SUCCESS");
            return tx != null ? tx : method.invoke(account, args);
        }

        if ("balanceAsync".equals(name)) {
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("balance"), null));
        }

        if ("setAsync".equals(name) && args != null && args.length == 1) {
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("set", BigDecimal.class), args));
        }

        if ("withdrawAsync".equals(name) && args != null && args.length == 1) {
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("withdraw", BigDecimal.class), args));
        }

        if ("depositAsync".equals(name) && args != null && args.length == 1) {
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("deposit", BigDecimal.class), args));
        }

        if ("transferAsync".equals(name) && args != null && args.length == 2) {
            Class<?> accountType = Class.forName("net.impactdev.impactor.api.economy.accounts.Account");
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("transfer", accountType, BigDecimal.class), args));
        }

        if ("resetAsync".equals(name)) {
            return CompletableFuture.completedFuture(invoke(account, account.getClass().getMethod("reset"), null));
        }

        return method.invoke(account, args);
    }

    private static UUID owner(Object account) {
        if (account == null) {
            return null;
        }
        try {
            Object owner = account.getClass().getMethod("owner").invoke(account);
            return owner instanceof UUID uuid ? uuid : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
