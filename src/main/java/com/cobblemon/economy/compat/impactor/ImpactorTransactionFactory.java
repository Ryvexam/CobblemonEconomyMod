package com.cobblemon.economy.compat.impactor;

import java.math.BigDecimal;
import java.time.Instant;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public final class ImpactorTransactionFactory {
    private ImpactorTransactionFactory() {
    }

    public static Object buildEconomyTransaction(Object account, BigDecimal amount, String transactionType, String resultType) {
        try {
            Class<?> builderClass = Class.forName("net.impactdev.impactor.core.economy.transactions.ImpactorEconomyTransaction$TransactionBuilder");
            Object builder = builderClass.getConstructor().newInstance();

            Object currency = account.getClass().getMethod("currency").invoke(account);
            Object type = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType", transactionType);
            Object result = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType", resultType);

            builderClass.getMethod("account", Class.forName("net.impactdev.impactor.api.economy.accounts.Account")).invoke(builder, account);
            builderClass.getMethod("currency", Class.forName("net.impactdev.impactor.api.economy.currency.Currency")).invoke(builder, currency);
            builderClass.getMethod("amount", BigDecimal.class).invoke(builder, amount);
            builderClass.getMethod("type", Class.forName("net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType")).invoke(builder, type);
            builderClass.getMethod("result", Class.forName("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType")).invoke(builder, result);
            builderClass.getMethod("timestamp", Instant.class).invoke(builder, Instant.now());
            builderClass.getMethod("message", Supplier.class).invoke(builder, (Supplier<Object>) () -> null);
            return builderClass.getMethod("build").invoke(builder);
        } catch (Exception ignored) {
        }
        return fallbackEconomyTransaction(account, amount, transactionType, resultType);
    }

    public static Object buildTransferTransaction(Object from, Object to, BigDecimal amount, String resultType) {
        try {
            Class<?> builderClass = Class.forName("net.impactdev.impactor.core.economy.transactions.ImpactorEconomyTransferTransaction$TransactionBuilder");
            Object builder = builderClass.getConstructor().newInstance();

            Object currency = from.getClass().getMethod("currency").invoke(from);
            Object result = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType", resultType);

            builderClass.getMethod("currency", Class.forName("net.impactdev.impactor.api.economy.currency.Currency")).invoke(builder, currency);
            builderClass.getMethod("from", Class.forName("net.impactdev.impactor.api.economy.accounts.Account")).invoke(builder, from);
            builderClass.getMethod("to", Class.forName("net.impactdev.impactor.api.economy.accounts.Account")).invoke(builder, to);
            builderClass.getMethod("amount", BigDecimal.class).invoke(builder, amount);
            builderClass.getMethod("result", Class.forName("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType")).invoke(builder, result);
            builderClass.getMethod("message", Supplier.class).invoke(builder, (Supplier<Object>) () -> null);
            return builderClass.getMethod("build").invoke(builder);
        } catch (Exception ignored) {
        }
        return fallbackTransferTransaction(from, to, amount, resultType);
    }

    private static Object fallbackEconomyTransaction(Object account, BigDecimal amount, String transactionType, String resultType) {
        try {
            Class<?> txInterface = Class.forName("net.impactdev.impactor.api.economy.transactions.EconomyTransaction");
            Object currency = account.getClass().getMethod("currency").invoke(account);
            Object type = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType", transactionType);
            Object result = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType", resultType);

            return Proxy.newProxyInstance(
                    txInterface.getClassLoader(),
                    new Class<?>[]{txInterface},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "account" -> account;
                        case "currency" -> currency;
                        case "amount" -> amount;
                        case "type" -> type;
                        case "result" -> result;
                        case "timestamp" -> Instant.now();
                        case "message" -> (Supplier<Object>) () -> null;
                        case "successful" -> "SUCCESS".equals(resultType);
                        case "inform" -> null;
                        default -> null;
                    }
            );
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object fallbackTransferTransaction(Object from, Object to, BigDecimal amount, String resultType) {
        try {
            Class<?> txInterface = Class.forName("net.impactdev.impactor.api.economy.transactions.EconomyTransferTransaction");
            Object currency = from.getClass().getMethod("currency").invoke(from);
            Object result = enumValue("net.impactdev.impactor.api.economy.transactions.details.EconomyResultType", resultType);

            return Proxy.newProxyInstance(
                    txInterface.getClassLoader(),
                    new Class<?>[]{txInterface},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "currency" -> currency;
                        case "from" -> from;
                        case "to" -> to;
                        case "amount" -> amount;
                        case "result" -> result;
                        case "message" -> (Supplier<Object>) () -> null;
                        case "successful" -> "SUCCESS".equals(resultType);
                        case "inform" -> null;
                        default -> null;
                    }
            );
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object enumValue(String enumClassName, String name) throws Exception {
        Class<?> enumClass = Class.forName(enumClassName);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object value = Enum.valueOf((Class<? extends Enum>) enumClass, name);
        return value;
    }
}
