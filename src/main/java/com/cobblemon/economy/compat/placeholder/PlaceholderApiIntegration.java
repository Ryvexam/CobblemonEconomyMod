package com.cobblemon.economy.compat.placeholder;

import com.cobblemon.economy.events.CobblemonListeners;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PlaceholderApiIntegration {
    private static final String[] API_CLASS_NAMES = new String[] {
        "eu.pb4.placeholders.api.Placeholders",
        "eu.pb4.placeholder.api.PlaceholderApi"
    };

    private static final String[] HANDLER_CLASS_NAMES = new String[] {
        "eu.pb4.placeholders.api.PlaceholderHandler",
        "eu.pb4.placeholder.api.Placeholder"
    };

    private static final String[] RESULT_CLASS_NAMES = new String[] {
        "eu.pb4.placeholders.api.PlaceholderResult",
        "eu.pb4.placeholder.api.PlaceholderResult"
    };

    private static final String[] PLACEHOLDER_NAMESPACES = new String[] {
        "cobeco"
    };

    private static final String[] PLACEHOLDERS = new String[] {
        "balance",
        "balance_symbol",
        "pco",
        "pco_symbol",
        "unique_captures"
    };

    private PlaceholderApiIntegration() {
    }

    public static void register() {
        if (!isPlaceholderApiLoaded()) {
            return;
        }

        try {
            Class<?> placeholderApiClass = loadFirstPresentClass(API_CLASS_NAMES);
            Class<?> placeholderClass = loadFirstPresentClass(HANDLER_CLASS_NAMES);
            Class<?> placeholderResultClass = loadFirstPresentClass(RESULT_CLASS_NAMES);

            if (placeholderApiClass == null || placeholderClass == null || placeholderResultClass == null) {
                CobblemonEconomy.LOGGER.warn("Placeholder API detected but compatible API classes were not found.");
                return;
            }

            Method registerMethod = findRegisterMethod(placeholderApiClass, placeholderClass);
            if (registerMethod == null) {
                CobblemonEconomy.LOGGER.warn("Placeholder API detected but register method not found.");
                return;
            }

            for (String namespace : PLACEHOLDER_NAMESPACES) {
                for (String placeholderId : PLACEHOLDERS) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, placeholderId);
                    Object placeholder = createPlaceholder(placeholderClass, placeholderResultClass, placeholderId);
                    registerMethod.invoke(null, id, placeholder);
                }
            }

            CobblemonEconomy.LOGGER.info("Placeholder API integration enabled.");
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.warn("Failed to register Placeholder API integration: {}", e.getMessage());
        }
    }

    private static boolean isPlaceholderApiLoaded() {
        return FabricLoader.getInstance().isModLoaded("placeholder-api")
            || FabricLoader.getInstance().isModLoaded("placeholderapi");
    }

    private static Method findRegisterMethod(Class<?> apiClass, Class<?> placeholderClass) {
        for (Method method : apiClass.getMethods()) {
            if (!method.getName().equals("register") || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2) {
                continue;
            }
            if (params[1].isAssignableFrom(placeholderClass)) {
                return method;
            }
        }
        return null;
    }

    private static Class<?> loadFirstPresentClass(String[] classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static Object createPlaceholder(Class<?> placeholderClass, Class<?> placeholderResultClass, String placeholderId) {
        return Proxy.newProxyInstance(
            placeholderClass.getClassLoader(),
            new Class<?>[] { placeholderClass },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return handleObjectMethod(proxy, method.getName(), args);
                }

                Object context = args != null && args.length > 0 ? args[0] : null;
                ServerPlayer player = extractPlayer(context);
                String result = resolvePlaceholderValue(player, placeholderId);
                return buildPlaceholderResult(placeholderResultClass, Component.literal(result));
            }
        );
    }

    private static Object handleObjectMethod(Object proxy, String name, Object[] args) {
        return switch (name) {
            case "toString" -> "CobblemonEconomyPlaceholder";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
            default -> null;
        };
    }

    private static ServerPlayer extractPlayer(Object context) {
        if (context == null) {
            return null;
        }

        ServerPlayer player = (ServerPlayer) invokeContextMethod(context, "player", ServerPlayer.class);
        if (player != null) {
            return player;
        }

        player = (ServerPlayer) invokeContextMethod(context, "getPlayer", ServerPlayer.class);
        if (player != null) {
            return player;
        }

        Object entity = invokeContextMethod(context, "entity", Object.class);
        if (entity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        entity = invokeContextMethod(context, "getEntity", Object.class);
        if (entity instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        return null;
    }

    private static Object invokeContextMethod(Object context, String methodName, Class<?> expectedType) {
        try {
            Method method = context.getClass().getMethod(methodName);
            Object value = method.invoke(context);
            if (value != null && expectedType.isInstance(value)) {
                return value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String resolvePlaceholderValue(ServerPlayer player, String placeholderId) {
        EconomyManager manager = CobblemonEconomy.getEconomyManager();
        if (manager == null || player == null) {
            return "0";
        }

        boolean withSymbol = placeholderId.endsWith("_symbol");
        boolean isPco = placeholderId.startsWith("pco");
        boolean uniqueCapture = placeholderId.startsWith("unique_capture");

        if (uniqueCapture) {
            int count = CobblemonListeners.getUniqueCaptureCount(player);
            if (count >= 0) {
                manager.setCaptureCount(player.getUUID(), count);
                return String.valueOf(count);
            }
            return String.valueOf(manager.getCaptureCount(player.getUUID()));
        }

        BigDecimal value = isPco
            ? manager.getPco(player.getUUID())
            : manager.getBalance(player.getUUID());

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

    private static Object buildPlaceholderResult(Class<?> placeholderResultClass, Component component) {
        List<String> methodNames = List.of("value", "of", "create");
        for (String methodName : methodNames) {
            Object result = tryBuildResult(placeholderResultClass, component, methodName);
            if (result != null) {
                return result;
            }
        }

        Object result = tryBuildResultWithString(placeholderResultClass, component.getString());
        if (result != null) {
            return result;
        }

        Object fallback = tryGetStaticField(placeholderResultClass, "EMPTY");
        if (fallback != null) {
            return fallback;
        }

        return null;
    }

    private static Object tryBuildResult(Class<?> placeholderResultClass, Component component, String methodName) {
        for (Method method : placeholderResultClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(Component.class)) {
                try {
                    return method.invoke(null, component);
                } catch (Exception ignored) {
                }
            }
            if (params.length == 2 && params[0].isAssignableFrom(Component.class) && params[1] == boolean.class) {
                try {
                    return method.invoke(null, component, false);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static Object tryBuildResultWithString(Class<?> placeholderResultClass, String value) {
        for (Method method : placeholderResultClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("value")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == String.class) {
                try {
                    return method.invoke(null, value);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static Object tryGetStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            if (Modifier.isStatic(field.getModifiers())) {
                return field.get(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
