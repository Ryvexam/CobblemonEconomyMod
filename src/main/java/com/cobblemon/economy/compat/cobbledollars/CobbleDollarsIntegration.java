package com.cobblemon.economy.compat.cobbledollars;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;
import java.lang.reflect.Method;

public final class CobbleDollarsIntegration {
    private CobbleDollarsIntegration() {
    }

    public static boolean register() {
        boolean loaded = FabricLoader.getInstance().isModLoaded("cobbledollars");
        if (loaded) {
            CobblemonEconomy.LOGGER.info("CobbleDollars compatibility enabled.");
        }
        return loaded;
    }

    public static boolean canAccess(ServerPlayer player) {
        return findGetter(player) != null && (findSpend(player) != null || findSetter(player) != null);
    }

    public static Integer getBalance(ServerPlayer player) {
        try {
            Method getter = findGetter(player);
            if (getter == null) {
                return null;
            }
            Object value = getter.invoke(player);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean spend(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        Integer current = getBalance(player);
        if (current == null || current < amount) {
            return false;
        }

        try {
            Method spend = findSpend(player);
            if (spend != null) {
                Class<?> param = spend.getParameterTypes()[0];
                if (param == int.class || param == Integer.class) {
                    spend.invoke(player, amount);
                } else if (param == long.class || param == Long.class) {
                    spend.invoke(player, (long) amount);
                } else if (param == BigInteger.class) {
                    spend.invoke(player, BigInteger.valueOf(amount));
                } else {
                    return false;
                }
                return true;
            }

            Method set = findSetter(player);
            if (set != null) {
                int newBalance = current - amount;
                Class<?> param = set.getParameterTypes()[0];
                if (param == int.class || param == Integer.class) {
                    set.invoke(player, newBalance);
                } else if (param == long.class || param == Long.class) {
                    set.invoke(player, (long) newBalance);
                } else if (param == BigInteger.class) {
                    set.invoke(player, BigInteger.valueOf(newBalance));
                } else {
                    return false;
                }
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean set(ServerPlayer player, int amount) {
        if (player == null || amount < 0) {
            return false;
        }

        try {
            Method set = findSetter(player);
            if (set == null) {
                return false;
            }

            Class<?> param = set.getParameterTypes()[0];
            if (param == int.class || param == Integer.class) {
                set.invoke(player, amount);
            } else if (param == long.class || param == Long.class) {
                set.invoke(player, (long) amount);
            } else if (param == BigInteger.class) {
                set.invoke(player, BigInteger.valueOf(amount));
            } else {
                return false;
            }
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public static boolean earn(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        try {
            Method earn = getMethod(player, "earnCobbleDollars", int.class);
            if (earn != null) {
                earn.invoke(player, amount);
                return true;
            }
            earn = getMethod(player, "earnCobbleDollars", long.class);
            if (earn != null) {
                earn.invoke(player, (long) amount);
                return true;
            }
            earn = getMethod(player, "earnCobbleDollars", BigInteger.class);
            if (earn != null) {
                earn.invoke(player, BigInteger.valueOf(amount));
                return true;
            }
            earn = getMethod(player, "cobbleDollars$earnCobbleDollars", int.class);
            if (earn != null) {
                earn.invoke(player, amount);
                return true;
            }
            earn = getMethod(player, "cobbleDollars$earnCobbleDollars", long.class);
            if (earn != null) {
                earn.invoke(player, (long) amount);
                return true;
            }
            earn = getMethod(player, "cobbleDollars$earnCobbleDollars", BigInteger.class);
            if (earn != null) {
                earn.invoke(player, BigInteger.valueOf(amount));
                return true;
            }
        } catch (Exception ignored) {
        }

        Integer current = getBalance(player);
        if (current == null) {
            return false;
        }
        return set(player, current + amount);
    }

    private static Method getMethod(ServerPlayer player, String name, Class<?>... parameterTypes) {
        if (player == null) {
            return null;
        }
        try {
            Method method = player.getClass().getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Method findGetter(ServerPlayer player) {
        Method method = getMethod(player, "getCobbleDollars");
        if (method != null) {
            return method;
        }
        return getMethod(player, "cobbleDollars$getCobbleDollars");
    }

    private static Method findSpend(ServerPlayer player) {
        Method method = getMethod(player, "spendCobbleDollars", int.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "spendCobbleDollars", long.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "spendCobbleDollars", BigInteger.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "cobbleDollars$spendCobbleDollars", int.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "cobbleDollars$spendCobbleDollars", long.class);
        if (method != null) {
            return method;
        }
        return getMethod(player, "cobbleDollars$spendCobbleDollars", BigInteger.class);
    }

    private static Method findSetter(ServerPlayer player) {
        Method method = getMethod(player, "setCobbleDollars", int.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "setCobbleDollars", long.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "setCobbleDollars", BigInteger.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "cobbleDollars$setCobbleDollars", int.class);
        if (method != null) {
            return method;
        }
        method = getMethod(player, "cobbleDollars$setCobbleDollars", long.class);
        if (method != null) {
            return method;
        }
        return getMethod(player, "cobbleDollars$setCobbleDollars", BigInteger.class);
    }
}
