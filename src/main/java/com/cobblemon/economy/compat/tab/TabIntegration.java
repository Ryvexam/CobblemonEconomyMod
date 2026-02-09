package com.cobblemon.economy.compat.tab;

import com.cobblemon.economy.events.CobblemonListeners;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class TabIntegration {
    private static final String[] PLACEHOLDERS = new String[] {
        "%cobeco_balance%",
        "%cobeco_balance_symbol%",
        "%cobeco_pco%",
        "%cobeco_pco_symbol%",
        "%cobeco_unique_captures%"
    };
    private static final Set<String> PLACEHOLDER_KEYS = new HashSet<>();
    private static boolean eventsHooked = false;
    private static Object placeholderRegisterHandlerRef;
    private static Object tabLoadHandlerRef;

    static {
        for (String placeholder : PLACEHOLDERS) {
            PLACEHOLDER_KEYS.add(toKey(placeholder));
        }
    }

    private TabIntegration() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("tab")) {
            return;
        }

        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
            if (tabApi == null) {
                CobblemonEconomy.LOGGER.warn("TAB detected but TabAPI instance is not available yet.");
                return;
            }

            hookTabEvents(tabApi, tabApiClass);

            Object placeholderManager = tabApiClass.getMethod("getPlaceholderManager").invoke(tabApi);
            Method registerPlayerPlaceholder = placeholderManager.getClass().getMethod(
                "registerPlayerPlaceholder",
                String.class,
                int.class,
                Function.class
            );
            Method getPlaceholder = placeholderManager.getClass().getMethod("getPlaceholder", String.class);
            Method unregisterPlaceholderByObject = placeholderManager.getClass().getMethod("unregisterPlaceholder", Class.forName("me.neznamy.tab.api.placeholder.Placeholder"));

            for (String placeholder : PLACEHOLDERS) {
                Object existing = getPlaceholder.invoke(placeholderManager, placeholder);
                if (existing != null) {
                    unregisterPlaceholderByObject.invoke(placeholderManager, existing);
                }

                Function<Object, Object> provider = tabPlayer -> resolveValue(tabPlayer, placeholder);
                registerPlayerPlaceholder.invoke(placeholderManager, placeholder, 1000, provider);
            }

            CobblemonEconomy.LOGGER.info("TAB placeholder integration enabled.");
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.warn("Failed to register TAB placeholder integration: {}", e.getMessage());
        }
    }

    private static void hookTabEvents(Object tabApi, Class<?> tabApiClass) {
        if (eventsHooked) {
            return;
        }

        try {
            Object eventBus = tabApiClass.getMethod("getEventBus").invoke(tabApi);
            Class<?> eventBusClass = Class.forName("me.neznamy.tab.api.event.EventBus");
            Class<?> eventHandlerClass = Class.forName("me.neznamy.tab.api.event.EventHandler");
            Class<?> placeholderRegisterEventClass = Class.forName("me.neznamy.tab.api.event.plugin.PlaceholderRegisterEvent");
            Class<?> tabLoadEventClass = Class.forName("me.neznamy.tab.api.event.plugin.TabLoadEvent");
            Method registerTyped = eventBusClass.getMethod("register", Class.class, eventHandlerClass);

            placeholderRegisterHandlerRef = Proxy.newProxyInstance(
                eventHandlerClass.getClassLoader(),
                new Class<?>[] { eventHandlerClass },
                (proxy, method, args) -> {
                    if ("handle".equals(method.getName()) && args != null && args.length > 0) {
                        handlePlaceholderRegisterEvent(args[0]);
                    }
                    return null;
                }
            );

            tabLoadHandlerRef = Proxy.newProxyInstance(
                eventHandlerClass.getClassLoader(),
                new Class<?>[] { eventHandlerClass },
                (proxy, method, args) -> {
                    if ("handle".equals(method.getName())) {
                        register();
                    }
                    return null;
                }
            );

            registerTyped.invoke(eventBus, placeholderRegisterEventClass, placeholderRegisterHandlerRef);
            registerTyped.invoke(eventBus, tabLoadEventClass, tabLoadHandlerRef);
            eventsHooked = true;
            CobblemonEconomy.LOGGER.info("TAB event hooks enabled.");
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.warn("Failed to hook TAB events: {}", e.getMessage());
        }
    }

    private static void handlePlaceholderRegisterEvent(Object event) {
        try {
            Method getIdentifier = event.getClass().getMethod("getIdentifier");
            String identifier = (String) getIdentifier.invoke(event);
            if (!PLACEHOLDER_KEYS.contains(toKey(identifier))) {
                return;
            }

            Method setPlayerPlaceholder = event.getClass().getMethod("setPlayerPlaceholder", Function.class);
            setPlayerPlaceholder.invoke(event, (Function<Object, Object>) tabPlayer -> resolveValue(tabPlayer, identifier));
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.warn("Failed to handle TAB placeholder event: {}", e.getMessage());
        }
    }

    private static String toKey(String placeholder) {
        if (placeholder == null) {
            return "";
        }
        String key = placeholder.trim();
        if (key.startsWith("%") && key.endsWith("%") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1);
        }
        return key.toLowerCase();
    }

    private static String resolveValue(Object tabPlayer, String placeholder) {
        EconomyManager manager = CobblemonEconomy.getEconomyManager();
        UUID uuid = extractUuid(tabPlayer);
        if (manager == null || uuid == null) {
            return "0";
        }

        boolean isPco = placeholder.contains("_pco");
        boolean withSymbol = placeholder.endsWith("_symbol%");
        boolean uniqueCapture = placeholder.contains("_unique_capture");

        if (uniqueCapture) {
            return String.valueOf(resolveUniqueCaptureCount(uuid, manager));
        }

        BigDecimal value = isPco ? manager.getPco(uuid) : manager.getBalance(uuid);
        String base = formatWholeNumber(value);

        if (!withSymbol) {
            return base;
        }
        return isPco ? base + " PCo" : base + "₽";
    }

    private static String formatWholeNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static int resolveUniqueCaptureCount(UUID uuid, EconomyManager manager) {
        MinecraftServer server = CobblemonEconomy.getGameServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                int live = CobblemonListeners.getUniqueCaptureCount(player);
                if (live >= 0) {
                    manager.setCaptureCount(uuid, live);
                    return live;
                }
            }
        }
        return manager.getCaptureCount(uuid);
    }

    private static UUID extractUuid(Object tabPlayer) {
        if (tabPlayer == null) {
            return null;
        }

        try {
            Method method = tabPlayer.getClass().getMethod("getUniqueId");
            Object value = method.invoke(tabPlayer);
            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
