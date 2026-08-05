package com.cobblemon.economy.shop;

import com.mojang.serialization.JsonOps;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyConfig;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import com.cobblemon.economy.storage.EconomyManager;
import com.cobblemon.economy.util.PerformanceProfiler;

public class ShopGui {
    private static final int ITEMS_PER_PAGE = 36; // Slots 9 to 44
    private static final Map<UUID, ResolvedShopSession> ACTIVE_SESSIONS = new HashMap<>();

    private static class ResolvedItem {
        ItemStack templateStack;
        Item item;
        int price;
        String name;
        int quantity = 1;
        final String originalId;
        final Map<String, JsonElement> components;
        final EconomyConfig.ShopItemDefinition definition;
        final boolean isCommand;

        ResolvedItem(EconomyConfig.ShopItemDefinition def, HolderLookup.Provider lookupProvider) {
            this.definition = def;
            this.originalId = def.id;
            this.components = def.components;
            this.price = def.price;
            this.name = def.name;
            this.isCommand = "command".equals(def.type);
            resolve(lookupProvider);
        }

        public void resolve(HolderLookup.Provider lookupProvider) {
            try {
                resolveInternal(lookupProvider);
            } catch (Exception e) {
                // A single broken entry must never prevent the whole shop from opening
                CobblemonEconomy.LOGGER.error("Failed to resolve shop item '{}'", originalId, e);
                if (this.item == null) {
                    this.item = Items.BARRIER;
                }
                if (this.templateStack == null || this.templateStack.isEmpty()) {
                    this.templateStack = new ItemStack(this.item);
                }
                if (this.name == null || this.name.isEmpty()) {
                    this.name = originalId;
                }
            }
        }

        private void resolveInternal(HolderLookup.Provider lookupProvider) {
            Random rand = new Random();

            // Handle command type items with custom display
            if (isCommand) {
                resolveCommandItem(lookupProvider);
                return;
            }

            // 1. Resolve the Item ID
            // The id may carry vanilla /give style components: "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]"
            String normalizedId = normalizeItemId(originalId);
            String inlineComponents = extractInlineComponents(normalizedId);
            normalizedId = stripInlineComponents(normalizedId);
            if (normalizedId.contains(":*")) {
                String namespace = normalizedId.split(":")[0];
                List<Item> candidates = BuiltInRegistries.ITEM.stream()
                        .filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(namespace))
                        .toList();

                this.item = candidates.isEmpty() ? Items.BARRIER : candidates.get(rand.nextInt(candidates.size()));
                
                // Random price +/- 25% for wildcards
                double multiplier = 0.75 + (rand.nextDouble() * 0.5);
                this.price = (int) Math.round(this.definition.price * multiplier);
            } else {
                ResourceLocation loc = ResourceLocation.tryParse(normalizedId);
                this.item = loc == null ? Items.AIR : BuiltInRegistries.ITEM.get(loc);

                if (this.item == Items.AIR && !normalizedId.equals("minecraft:air")) {
                    CobblemonEconomy.LOGGER.error("Invalid item ID: {}", originalId);
                    this.item = Items.BARRIER;
                }
            }

            // Fallback for name if not provided in config
            if (this.name == null || this.name.isEmpty()) {
                this.name = Component.translatable(this.item.getDescriptionId()).getString();
            }

            this.templateStack = new ItemStack(this.item);

            // 2. Components declared inline on the id (vanilla /give syntax)
            if (!inlineComponents.isEmpty()) {
                applyInlineComponents(this.templateStack, inlineComponents, lookupProvider);
            }

            // 3. Shorthand fields + the "components" map (map entries win on conflict)
            Map<String, JsonElement> jsonComponents = buildComponentJson(this.definition, this.components);
            if (!jsonComponents.isEmpty()) {
                applyComponents(this.templateStack, jsonComponents, lookupProvider);
            }
        }

        private void resolveCommandItem(HolderLookup.Provider lookupProvider) {
            // For command items, use displayItem config or fallback to default
            String inlineComponents = "";
            if (definition.displayItem != null && definition.displayItem.material != null) {
                String material = normalizeItemId(definition.displayItem.material);
                inlineComponents = extractInlineComponents(material);
                ResourceLocation loc = ResourceLocation.tryParse(stripInlineComponents(material));
                this.item = loc == null ? Items.AIR : BuiltInRegistries.ITEM.get(loc);
                if (this.item == Items.AIR) {
                    CobblemonEconomy.LOGGER.error("Invalid display item material: {}", definition.displayItem.material);
                    this.item = Items.COMMAND_BLOCK;
                }
            } else {
                this.item = Items.COMMAND_BLOCK; // Default icon for command items
            }

            // Use display name from config if available
            if (definition.displayItem != null && definition.displayItem.displayname != null) {
                this.name = definition.displayItem.displayname;
            } else if (this.name == null || this.name.isEmpty()) {
                this.name = "Command";
            }

            this.templateStack = new ItemStack(this.item);

            // Apply enchantment effect if requested
            if (definition.displayItem != null && Boolean.TRUE.equals(definition.displayItem.enchantEffect)) {
                this.templateStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            // Command entries support the same component shorthands as normal items
            if (!inlineComponents.isEmpty()) {
                applyInlineComponents(this.templateStack, inlineComponents, lookupProvider);
            }
            Map<String, JsonElement> jsonComponents = buildComponentJson(this.definition, this.components);
            if (!jsonComponents.isEmpty()) {
                applyComponents(this.templateStack, jsonComponents, lookupProvider);
            }
        }

            private void applyComponents(ItemStack stack, Map<String, JsonElement> componentDataMap, HolderLookup.Provider lookupProvider) {
                for (Map.Entry<String, JsonElement> entry : componentDataMap.entrySet()) {
                    String componentId = entry.getKey();
                    JsonElement data = entry.getValue();

                    // Special Handling: custom_data given as an SNBT string ("{foo:1}").
                    // Written as a real JSON object it falls through to the normal codec path below.
                    if (componentId.equals("minecraft:custom_data")
                            && data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) {
                        try {
                            CompoundTag tag = TagParser.parseTag(data.getAsString());
                            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        } catch (Exception e) {
                            CobblemonEconomy.LOGGER.error("Failed to parse custom_data SNBT: {}", data, e);
                        }
                        continue;
                    }

                    // Standard Components: Look up type from registry
                    DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(componentId));

                    if (componentType != null) {
                        applyComponentHelper(stack, componentType, data, lookupProvider);
                    } else {
                        CobblemonEconomy.LOGGER.warn("Unknown component type: {}", componentId);
                    }
                }
            }

            private <T> void applyComponentHelper(ItemStack stack, DataComponentType<T> type, JsonElement json, HolderLookup.Provider lookupProvider) {
                // The Fix: Create RegistryOps so the Codec can look up Enchantments, etc.
                RegistryOps<JsonElement> ops = lookupProvider.createSerializationContext(JsonOps.INSTANCE);

                type.codecOrThrow().parse(ops, json)
                        .resultOrPartial(error -> CobblemonEconomy.LOGGER.error("Failed to parse component {}: {}", type, error))
                        .ifPresent(value -> stack.set(type, value));
            }
        }

    // ------------------------------------------------------------------
    // Config -> data components helpers
    //
    // Goal: never force server owners to write escaped JSON inside JSON.
    // Three equivalent ways to describe an enchanted sword:
    //   1. "enchantments": { "sharpness": 5 }
    //   2. "components": { "minecraft:enchantments": { "levels": { "minecraft:sharpness": 5 } } }
    //   3. "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]"
    // The old escaped-string form stays supported for existing configs.
    // ------------------------------------------------------------------

    /** Merges the shorthand fields and the "components" map into one id -> JSON map. Map entries win on conflict. */
    private static Map<String, JsonElement> buildComponentJson(EconomyConfig.ShopItemDefinition def,
                                                               Map<String, JsonElement> rawComponents) {
        Map<String, JsonElement> out = new LinkedHashMap<>();
        if (def != null) {
            JsonObject enchantments = buildEnchantmentComponent(def.enchantments);
            if (enchantments != null) {
                out.put("minecraft:enchantments", enchantments);
            }
            if (def.lore != null && !def.lore.isEmpty()) {
                JsonArray lore = new JsonArray();
                for (String line : def.lore) {
                    if (line != null) {
                        lore.add(line);
                    }
                }
                if (!lore.isEmpty()) {
                    out.put("minecraft:lore", lore);
                }
            }
            if (Boolean.TRUE.equals(def.unbreakable)) {
                out.put("minecraft:unbreakable", new JsonObject());
            }
            if (def.customModelData != null) {
                out.put("minecraft:custom_model_data", new JsonPrimitive(def.customModelData));
            }
            if (def.glint != null) {
                out.put("minecraft:enchantment_glint_override", new JsonPrimitive(def.glint));
            }
        }

        if (rawComponents != null) {
            for (Map.Entry<String, JsonElement> entry : rawComponents.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isJsonNull()) {
                    continue;
                }
                String key = normalizeComponentKey(entry.getKey());
                // custom_data strings stay untouched: they are SNBT, parsed further down
                JsonElement value = key.equals("minecraft:custom_data")
                        ? entry.getValue()
                        : normalizeComponentValue(entry.getValue());
                out.put(key, value);
            }
        }
        return out;
    }

    /**
     * Accepts every reasonable way of writing enchantments and returns the vanilla component shape:
     *   { "sharpness": 5 }                       -> { "levels": { "minecraft:sharpness": 5 } }
     *   ["sharpness 5", "unbreaking 3"]          -> { "levels": { ... } }
     *   [{ "id": "sharpness", "level": 5 }]      -> { "levels": { ... } }
     *   { "levels": { "minecraft:sharpness": 5 } } -> passed through (keys normalized)
     */
    private static JsonObject buildEnchantmentComponent(JsonElement source) {
        if (source == null || source.isJsonNull()) {
            return null;
        }

        JsonObject levels = new JsonObject();
        JsonObject extra = new JsonObject();

        if (source.isJsonObject()) {
            JsonObject obj = source.getAsJsonObject();
            if (obj.has("levels") && obj.get("levels").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    if (!entry.getKey().equals("levels")) {
                        extra.add(entry.getKey(), entry.getValue());
                    }
                }
                for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("levels").entrySet()) {
                    addEnchantment(levels, entry.getKey(), entry.getValue());
                }
            } else {
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    addEnchantment(levels, entry.getKey(), entry.getValue());
                }
            }
        } else if (source.isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray()) {
                addEnchantmentEntry(levels, element);
            }
        } else {
            addEnchantmentEntry(levels, source);
        }

        if (levels.size() == 0) {
            CobblemonEconomy.LOGGER.warn("Ignoring empty or unreadable 'enchantments' value: {}", source);
            return null;
        }

        JsonObject component = new JsonObject();
        component.add("levels", levels);
        for (Map.Entry<String, JsonElement> entry : extra.entrySet()) {
            component.add(entry.getKey(), entry.getValue());
        }
        return component;
    }

    /** Handles a single list entry: "sharpness 5", "sharpness", or { "id": "sharpness", "level": 5 }. */
    private static void addEnchantmentEntry(JsonObject levels, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonElement id = obj.has("id") ? obj.get("id") : obj.get("enchantment");
            if (id == null || !id.isJsonPrimitive()) {
                CobblemonEconomy.LOGGER.warn("Ignoring enchantment entry without an id: {}", element);
                return;
            }
            addEnchantment(levels, id.getAsString(), obj.has("level") ? obj.get("level") : obj.get("lvl"));
            return;
        }
        if (!element.isJsonPrimitive()) {
            CobblemonEconomy.LOGGER.warn("Ignoring unreadable enchantment entry: {}", element);
            return;
        }

        String text = element.getAsString().trim();
        if (text.isEmpty()) {
            return;
        }
        // "sharpness 5" / "sharpness:5" / "sharpness"
        String name = text;
        String level = null;
        int space = text.lastIndexOf(' ');
        if (space > 0) {
            name = text.substring(0, space).trim();
            level = text.substring(space + 1).trim();
        } else {
            int lastColon = text.lastIndexOf(':');
            if (lastColon > 0 && isInteger(text.substring(lastColon + 1))) {
                name = text.substring(0, lastColon).trim();
                level = text.substring(lastColon + 1).trim();
            }
        }
        addEnchantment(levels, name, level == null ? null : new JsonPrimitive(level));
    }

    private static void addEnchantment(JsonObject levels, String name, JsonElement level) {
        String id = normalizeEnchantmentId(name);
        if (id == null) {
            return;
        }
        levels.addProperty(id, readLevel(level, name));
    }

    private static int readLevel(JsonElement level, String enchantmentName) {
        if (level == null || level.isJsonNull()) {
            return 1;
        }
        if (level.isJsonPrimitive()) {
            JsonPrimitive primitive = level.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsInt();
            }
            if (primitive.isString() && isInteger(primitive.getAsString())) {
                return Integer.parseInt(primitive.getAsString().trim());
            }
        }
        CobblemonEconomy.LOGGER.warn("Unreadable level '{}' for enchantment '{}', using 1", level, enchantmentName);
        return 1;
    }

    private static String normalizeEnchantmentId(String name) {
        if (name == null) {
            return null;
        }
        String id = unquote(name.trim()).toLowerCase(Locale.ROOT).replace(' ', '_');
        if (id.isEmpty()) {
            return null;
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String normalizeComponentKey(String key) {
        String id = unquote(key.trim());
        return id.contains(":") ? id : "minecraft:" + id;
    }

    /**
     * Keeps backwards compatibility with the old "component value is an escaped JSON string" format
     * while letting new configs write plain values. A string that clearly looks like JSON is parsed,
     * anything else is kept as a literal string.
     */
    private static JsonElement normalizeComponentValue(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return value;
        }
        String raw = value.getAsString().trim();
        if (raw.isEmpty()) {
            return value;
        }
        char first = raw.charAt(0);
        boolean looksLikeJson = first == '{' || first == '[' || first == '"' || first == '\''
                || raw.equals("true") || raw.equals("false") || isNumber(raw);
        if (!looksLikeJson) {
            return value;
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed == null || parsed.isJsonNull() ? value : parsed;
        } catch (Exception e) {
            return value;
        }
    }

    /** Returns the content between the first '[' and the last ']' of an item id, or "" when there is none. */
    private static String extractInlineComponents(String itemId) {
        if (itemId == null) {
            return "";
        }
        int start = itemId.indexOf('[');
        int end = itemId.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return "";
        }
        return itemId.substring(start + 1, end).trim();
    }

    private static String stripInlineComponents(String itemId) {
        if (itemId == null) {
            return null;
        }
        int start = itemId.indexOf('[');
        return start < 0 ? itemId.trim() : itemId.substring(0, start).trim();
    }

    /** Applies vanilla /give style components written directly on the item id. */
    private static void applyInlineComponents(ItemStack stack, String inline, HolderLookup.Provider lookupProvider) {
        for (String entry : splitTopLevel(inline)) {
            if (entry.isEmpty()) {
                continue;
            }
            int separator = indexOfTopLevel(entry, '=');
            if (separator < 0 && entry.startsWith("!")) {
                DataComponentType<?> type = findComponentType(normalizeComponentKey(entry.substring(1)));
                if (type != null) {
                    stack.remove(type);
                }
                continue;
            }
            if (separator <= 0) {
                CobblemonEconomy.LOGGER.warn("Ignoring malformed inline component '{}'", entry);
                continue;
            }

            String key = normalizeComponentKey(entry.substring(0, separator));
            String rawValue = entry.substring(separator + 1).trim();
            DataComponentType<?> type = findComponentType(key);
            if (type == null) {
                continue;
            }
            try {
                Tag tag = TagParser.parseTag("{value:" + rawValue + "}").get("value");
                if (tag == null) {
                    CobblemonEconomy.LOGGER.error("Failed to read inline component {}: {}", key, rawValue);
                    continue;
                }
                applyComponentTag(stack, type, tag, lookupProvider);
            } catch (Exception e) {
                CobblemonEconomy.LOGGER.error("Failed to parse inline component {}={}", key, rawValue, e);
            }
        }
    }

    private static DataComponentType<?> findComponentType(String componentId) {
        ResourceLocation location = ResourceLocation.tryParse(componentId);
        DataComponentType<?> type = location == null ? null : BuiltInRegistries.DATA_COMPONENT_TYPE.get(location);
        if (type == null) {
            CobblemonEconomy.LOGGER.warn("Unknown component type: {}", componentId);
        }
        return type;
    }

    private static <T> void applyComponentTag(ItemStack stack, DataComponentType<T> type, Tag tag,
                                              HolderLookup.Provider lookupProvider) {
        RegistryOps<Tag> ops = lookupProvider.createSerializationContext(NbtOps.INSTANCE);
        type.codecOrThrow().parse(ops, tag)
                .resultOrPartial(error -> CobblemonEconomy.LOGGER.error("Failed to parse component {}: {}", type, error))
                .ifPresent(value -> stack.set(type, value));
    }

    /** Splits on commas that are not inside quotes, braces or brackets. */
    private static List<String> splitTopLevel(String input) {
        List<String> parts = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return parts;
        }
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (c == '\\' && i + 1 < input.length()) {
                    current.append(input.charAt(++i));
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                current.append(c);
            } else if (c == '{' || c == '[') {
                depth++;
                current.append(c);
            } else if (c == '}' || c == ']') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().isBlank()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    /** Index of the first occurrence of target outside quotes, braces and brackets. */
    private static int indexOfTopLevel(String input, char target) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == target && depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String unquote(String value) {
        if (value != null && value.length() >= 2) {
            char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizeItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return itemId;
        }
        return itemId
                .replace('\u00AB', '"')
                .replace('\u00BB', '"')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u201E', '"')
                .replace('\u201F', '"');
    }

    private static Class<?> tryClass(String... classNames) throws ClassNotFoundException {
        ClassNotFoundException lastError = null;
        for (String name : classNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    private static java.lang.reflect.Method tryMethod(Class<?> target, String[] methodNames, Class<?>... params) throws NoSuchMethodException {
        NoSuchMethodException lastError = null;
        for (String name : methodNames) {
            try {
                return target.getMethod(name, params);
            } catch (NoSuchMethodException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    private static java.lang.reflect.Method tryFactoryMethod(Class<?> target, String[] methodNames, Object registryAccess) throws NoSuchMethodException {
        return tryFactoryMethod(target, methodNames, registryAccess, null);
    }

    private static java.lang.reflect.Method tryFactoryMethod(Class<?> target, String[] methodNames, Object primaryArg, Object secondaryArg) throws NoSuchMethodException {
        for (String name : methodNames) {
            for (java.lang.reflect.Method method : target.getMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() == 0) {
                    return method;
                }
                if (method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (primaryArg != null && paramType.isAssignableFrom(primaryArg.getClass())) {
                        return method;
                    }
                    if (secondaryArg != null && paramType.isAssignableFrom(secondaryArg.getClass())) {
                        return method;
                    }
                }
            }
        }
        throw new NoSuchMethodException("No factory method found for " + target.getName());
    }

    private static java.lang.reflect.Method tryStackMethod(Class<?> target, String[] methodNames) throws NoSuchMethodException {
        for (String name : methodNames) {
            for (java.lang.reflect.Method method : target.getMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == int.class
                        && method.getParameterTypes()[1] == boolean.class) {
                    return method;
                }
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == int.class) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException("No stack builder method found for " + target.getName());
    }

    private static Object buildCommandContext(Object registryAccess) {
        try {
            Class<?> featureFlagsClass = tryClass(
                    "net.minecraft.world.flag.FeatureFlags",
                    "net.minecraft.class_7699"
            );
            Object flags = featureFlagsClass.getField("DEFAULT_FLAGS").get(null);

            Class<?> contextClass = tryClass(
                    "net.minecraft.commands.CommandBuildContext",
                    "net.minecraft.command.CommandRegistryAccess",
                    "net.minecraft.class_7157"
            );

            for (java.lang.reflect.Method method : contextClass.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!method.getName().equals("simple") && !method.getName().equals("create")) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 2
                        && params[0].isAssignableFrom(registryAccess.getClass())
                        && params[1].isAssignableFrom(flags.getClass())) {
                    return method.invoke(null, registryAccess, flags);
                }
                if (params.length == 1 && params[0].isAssignableFrom(registryAccess.getClass())) {
                    return method.invoke(null, registryAccess);
                }
            }

            for (java.lang.reflect.Constructor<?> constructor : contextClass.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2
                        && params[0].isAssignableFrom(registryAccess.getClass())
                        && params[1].isAssignableFrom(flags.getClass())) {
                    return constructor.newInstance(registryAccess, flags);
                }
                if (params.length == 1 && params[0].isAssignableFrom(registryAccess.getClass())) {
                    return constructor.newInstance(registryAccess);
                }
            }
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to build command context", e);
        }
        return null;
    }

    private static Object invokeFactory(java.lang.reflect.Method method, Object primaryArg, Object secondaryArg) throws Exception {
        if (method.getParameterCount() == 0) {
            return method.invoke(null);
        }
        if (method.getParameterCount() == 1) {
            Class<?> paramType = method.getParameterTypes()[0];
            if (primaryArg != null && paramType.isAssignableFrom(primaryArg.getClass())) {
                return method.invoke(null, primaryArg);
            }
            if (secondaryArg != null && paramType.isAssignableFrom(secondaryArg.getClass())) {
                return method.invoke(null, secondaryArg);
            }
        }
        throw new IllegalArgumentException("Unsupported factory method signature: " + method);
    }

    private static class ResolvedShopSession {
        final String shopId;
        final List<ResolvedItem> resolvedItems;

        ResolvedShopSession(String shopId, EconomyConfig.ShopDefinition shop, HolderLookup.Provider lookupProvider) {
            this.shopId = shopId;
            this.resolvedItems = new ArrayList<>();
            if (shop.items != null) {
                for (EconomyConfig.ShopItemDefinition itemDef : shop.items) {
                    if (itemDef != null && itemDef.id != null) {
                        this.resolvedItems.add(new ResolvedItem(itemDef, lookupProvider));
                    } else {
                        CobblemonEconomy.LOGGER.warn("Shop '{}' contains a null item or an item with missing ID!", shopId);
                    }
                }
            } else {
                CobblemonEconomy.LOGGER.warn("Shop '{}' has no items list defined!", shopId);
            }
        }
    }

    public static void open(ServerPlayer player, String shopId) {
        long profileStart = PerformanceProfiler.start();
        CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());
        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        if (shop == null) {
            shop = CobblemonEconomy.getConfig().shops.get("default_poke");
        }
        if (shop == null) return;

        // Create a new resolved session for this player open
        ResolvedShopSession session = new ResolvedShopSession(shopId, shop, player.registryAccess());
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        
        open(player, shopId, 0);
        PerformanceProfiler.end("shop_open_prepare", profileStart,
            PerformanceProfiler.format("player", player.getGameProfile().getName()) +
                ", " + PerformanceProfiler.format("shop", shopId) +
                ", " + PerformanceProfiler.format("items", session.resolvedItems.size()));
    }

    public static void open(ServerPlayer player, String shopId, int page) {
        long profileStart = PerformanceProfiler.start();
        ResolvedShopSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || !session.shopId.equals(shopId)) {
            open(player, shopId);
            return;
        }

        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        if (shop == null) return;

        boolean isPco = "PCO".equals(shop.currency);
        boolean isSell = shop.isSellShop;
        
        int totalItems = session.resolvedItems.size();
        int maxPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;
        
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        
        final int currentPage = page;
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < maxPages - 1;

        String titleChar;
        if (isSell) {
            if (hasPrev && hasNext) titleChar = isPco ? "\uE00E" : "\uE00A";
            else if (hasNext) titleChar = isPco ? "\uE00D" : "\uE009";
            else if (hasPrev) titleChar = isPco ? "\uE00F" : "\uE00B";
            else titleChar = isPco ? "\uE00C" : "\uE008";
        } else {
            if (hasPrev && hasNext) titleChar = isPco ? "\uE005" : "\uE004";
            else if (hasNext) titleChar = isPco ? "\uE003" : "\uE002";
            else if (hasPrev) titleChar = isPco ? "\uE007" : "\uE006";
            else titleChar = isPco ? "\uE001" : "\uE000";
        }

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
        
        String shopTitle = shop.title != null ? shop.title : "Shop";
        String negativeSpace = "\uF804".repeat(21);
        
        gui.setTitle(Component.literal("\uF804" + titleChar)
            .withStyle(style -> 
                style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))
                     .withColor(0xFFFFFF)
            )
            .append(Component.literal(negativeSpace)
                .withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))))
            .append(Component.literal(shopTitle)
                .withStyle(style -> style.withFont(ResourceLocation.withDefaultNamespace("default"))))
        );

        // Add linked shop switch button in slot 8 (top right corner)
        if (shop.linkedShop != null && !shop.linkedShop.isEmpty()) {
            EconomyConfig.ShopDefinition linkedShop = CobblemonEconomy.getConfig().shops.get(shop.linkedShop);
            if (linkedShop != null) {
                String linkedTitle = linkedShop.title != null ? linkedShop.title : "Linked Shop";
                Item switchIcon;
                if (shop.linkedShopIcon != null && !shop.linkedShopIcon.isEmpty()) {
                    // Use configured custom icon
                    switchIcon = BuiltInRegistries.ITEM.get(ResourceLocation.parse(shop.linkedShopIcon));
                } else {
                    // Fallback to default behavior
                    switchIcon = linkedShop.isSellShop ? Items.EMERALD : Items.GOLD_INGOT;
                }
                
                gui.setSlot(8, new GuiElementBuilder(switchIcon)
                    .setName(Component.translatable("cobblemon-economy.shop.switch_shop").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .addLoreLine(Component.literal(linkedTitle).withStyle(ChatFormatting.YELLOW))
                    .addLoreLine(Component.empty())
                    .addLoreLine(Component.translatable("cobblemon-economy.shop.switch_shop_lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .setCallback((index, type, action) -> {
                        player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
                        open(player, shop.linkedShop, 0);
                    })
                );
            }
        }

        if (hasPrev) {
            gui.setSlot(0, new GuiElementBuilder(Items.AIR)
                .setName(Component.translatable("cobblemon-economy.shop.return_to_start"))
                .setCallback((index, type, action) -> open(player, shopId, 0))
            );
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int slotIndex = 9 + i;
            int itemIndex = startIndex + i;

            if (itemIndex < endIndex) {
                ResolvedItem resolved = session.resolvedItems.get(itemIndex);
                
                Component actionLabel = Component.translatable(
                        isSell ? "cobblemon-economy.shop.sell_action" : "cobblemon-economy.shop.buy_action",
                        isSell ? resolved.quantity : resolved.quantity, // First arg for Sell (Sell X), ignored for Buy? Wait, Buy needs logic.
                        // Actually, let's make it generic.
                        // Buy: "Left: Buy | Middle: x{quantity}"
                        // Sell: "Left: Sell {quantity} | Right: Sell All | Middle: x{quantity}"
                        resolved.quantity // Second arg for Sell (Middle click hint), or first for Buy (Middle click hint)
                );
                
                // Re-doing the logic cleaner:
                if (isSell) {
                     actionLabel = Component.translatable("cobblemon-economy.shop.sell_action", resolved.quantity, resolved.quantity);
                } else {
                     actionLabel = Component.translatable("cobblemon-economy.shop.buy_action", resolved.quantity);
                }

                Component priceLabel = Component.translatable(isSell ? "cobblemon-economy.shop.sell_label" : "cobblemon-economy.shop.price_label");
                ChatFormatting priceColor = isPco ? ChatFormatting.AQUA : ChatFormatting.GREEN;

                // Calculate total price for current quantity
                long totalPrice = (long) resolved.price * resolved.quantity;
                
                ItemStack displayStack = resolved.templateStack.copy();
                displayStack.setCount(resolved.quantity);

                EconomyManager.PurchaseLimitStatus limitStatus = null;
                if (!isSell && resolved.definition.buyLimit != null && resolved.definition.buyLimit > 0) {
                    limitStatus = CobblemonEconomy.getEconomyManager().getPurchaseLimitStatus(
                            player.getUUID(),
                            shopId,
                            resolved.definition.id,
                            resolved.definition.buyLimit,
                            resolved.definition.buyCooldownMinutes
                    );
                } else if (isSell && resolved.definition.sellLimit != null && resolved.definition.sellLimit > 0) {
                    limitStatus = CobblemonEconomy.getEconomyManager().getSellLimitStatus(
                            player.getUUID(),
                            shopId,
                            resolved.definition.id,
                            resolved.definition.sellLimit,
                            resolved.definition.sellCooldownMinutes
                    );
                }

                GuiElementBuilder elementBuilder = new GuiElementBuilder(displayStack)
                    .setName(Component.literal(resolved.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .addLoreLine(priceLabel.copy().withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(totalPrice + (isPco ? " PCo" : "₽")).withStyle(priceColor)))
                    .addLoreLine(Component.literal("x" + resolved.quantity).withStyle(ChatFormatting.WHITE))
                    .addLoreLine(Component.empty());

                if (limitStatus != null && limitStatus.enabled) {
                    elementBuilder.addLoreLine(Component.translatable(
                                    "cobblemon-economy.shop.limit_status",
                                    limitStatus.remaining,
                                    isSell ? resolved.definition.sellLimit : resolved.definition.buyLimit
                            ).withStyle(ChatFormatting.GRAY));
                    if (limitStatus.resetAtMillis > 0) {
                        long remainingMs = Math.max(0, limitStatus.resetAtMillis - System.currentTimeMillis());
                        elementBuilder.addLoreLine(Component.translatable(
                                        "cobblemon-economy.shop.limit_resets_in",
                                        formatDuration(remainingMs)
                                ).withStyle(ChatFormatting.DARK_GRAY));
                    }
                }

                elementBuilder.addLoreLine(actionLabel.copy().withStyle(ChatFormatting.YELLOW))
                    // Removed the separate middle click line, integrated into actionLabel
                    .setCallback((index, type, action) -> {
                        if (type.isMiddle) {
                            // Rotate quantity: 1 -> 2 -> 4 -> 8 -> 16 -> 32 -> 64 -> 1
                            if (resolved.quantity >= 64) resolved.quantity = 1;
                            else resolved.quantity *= 2;
                            
                            // Play sound for feedback
                            player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
                        } else {
                            if (isSell) {
                                handleSell(player, resolved, isPco, type.isRight, shopId);
                            } else {
                                handlePurchase(player, resolved, isPco, shopId);
                            }
                        }
                        // Refresh GUI
                        open(player, shopId, currentPage); 
                    });

                gui.setSlot(slotIndex, elementBuilder);
            }
        }

        for (int i = 45; i <= 48; i++) {
            if (hasPrev) {
                final int prevPage = currentPage - 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> open(player, shopId, prevPage)));
            }
        }

        BigDecimal balance = isPco ? CobblemonEconomy.getEconomyManager().getPco(player.getUUID()) : CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        gui.setSlot(49, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setName(Component.translatable("cobblemon-economy.shop.balance_title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .setSkullOwner(player.getGameProfile(), player.server)
            .setLore(List.of(
                Component.literal(balance.stripTrailingZeros().toPlainString() + (isPco ? " PCo" : "₽")).withStyle(ChatFormatting.WHITE),
                Component.translatable("cobblemon-economy.shop.page_info", currentPage + 1, maxPages).withStyle(ChatFormatting.GRAY)
            ))
        );

        for (int i = 50; i <= 53; i++) {
            if (hasNext) {
                final int nextPage = currentPage + 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> open(player, shopId, nextPage)));
            }
        }

        gui.open();
        PerformanceProfiler.end("shop_open_render", profileStart,
            PerformanceProfiler.format("player", player.getGameProfile().getName()) +
                ", " + PerformanceProfiler.format("shop", shopId) +
                ", " + PerformanceProfiler.format("page", currentPage + 1) +
                ", " + PerformanceProfiler.format("items", totalItems));
    }

    private static void handlePurchase(ServerPlayer player, ResolvedItem resolved, boolean isPco, String shopId) {
        long profileStart = PerformanceProfiler.start();
        EconomyManager economyManager = CobblemonEconomy.getEconomyManager();
        EconomyConfig.ShopItemDefinition definition = resolved.definition;

        EconomyManager.PurchaseLimitStatus limitStatus = economyManager.getPurchaseLimitStatus(
                player.getUUID(),
                shopId,
                definition.id,
                definition.buyLimit,
                definition.buyCooldownMinutes
        );

        if (limitStatus.enabled && resolved.quantity > limitStatus.remaining) {
            long remainingMs = limitStatus.resetAtMillis > 0 ? Math.max(0, limitStatus.resetAtMillis - System.currentTimeMillis()) : 0;
            if (limitStatus.remaining <= 0) {
                MutableComponent message = Component.translatable("cobblemon-economy.shop.limit_reached").withStyle(ChatFormatting.RED);
                if (remainingMs > 0) {
                    message = message.append(Component.literal(" "))
                            .append(Component.translatable("cobblemon-economy.shop.limit_resets_in", formatDuration(remainingMs))
                                    .withStyle(ChatFormatting.GRAY));
                }
                player.sendSystemMessage(message);
            } else {
                MutableComponent message = Component.translatable("cobblemon-economy.shop.limit_remaining", limitStatus.remaining)
                        .withStyle(ChatFormatting.RED);
                if (remainingMs > 0) {
                    message = message.append(Component.literal(" "))
                            .append(Component.translatable("cobblemon-economy.shop.limit_resets_in", formatDuration(remainingMs))
                                    .withStyle(ChatFormatting.GRAY));
                }
                player.sendSystemMessage(message);
            }
            return;
        }

        // Inventory full check WIP (not working properly yet)
        // // Check if inventory is full
        // boolean inventoryFull = true;
        // for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
        //     ItemStack slot = player.getInventory().getItem(i);
        //     if (slot.isEmpty() || slot.getCount() < slot.getMaxStackSize()) {
        //         inventoryFull = false;
        //         break;
        //     }
        // }
        // if (inventoryFull) {
        //     player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.inventory_full").withStyle(ChatFormatting.RED));
        //     return;
        // }

        BigDecimal price = BigDecimal.valueOf(resolved.price).multiply(BigDecimal.valueOf(resolved.quantity));
        boolean success = isPco ? economyManager.subtractPco(player.getUUID(), price) : economyManager.subtractBalance(player.getUUID(), price);

        if (success) {
            if (limitStatus.enabled) {
                boolean consumed = economyManager.consumePurchaseLimit(
                        player.getUUID(),
                        shopId,
                        definition.id,
                        resolved.quantity,
                        definition.buyLimit,
                        definition.buyCooldownMinutes
                );
                if (!consumed) {
                    if (isPco) {
                        economyManager.addPco(player.getUUID(), price);
                    } else {
                        economyManager.addBalance(player.getUUID(), price);
                    }
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.limit_reached").withStyle(ChatFormatting.RED));
                    return;
                }
            }

            // Handle command type items
            if (resolved.isCommand) {
                if (definition.command != null && !definition.command.isEmpty()) {
                    for (int i = 0; i < resolved.quantity; i++) {
                        String cmd = definition.command.replace("%player%", player.getGameProfile().getName());
                        // Execute command as OP using server command source
                        player.server.getCommands().performPrefixedCommand(
                            player.server.createCommandSourceStack()
                                .withPermission(4) // OP permission level
                                .withSuppressedOutput(),
                            cmd
                        );
                    }
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.purchase_success", resolved.quantity + "x " + resolved.name).withStyle(ChatFormatting.GREEN));
                    player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
                    logTransaction(player, resolved, isPco, false, resolved.quantity, price);
                    resolved.resolve(player.registryAccess());
                    return;
                } else {
                    player.sendSystemMessage(Component.literal("No command configured for this item").withStyle(ChatFormatting.RED));
                    // Refund
                    if (isPco) {
                        economyManager.addPco(player.getUUID(), price);
                    } else {
                        economyManager.addBalance(player.getUUID(), price);
                    }
                    return;
                }
            }

            ItemStack stackToGive;

            // Check for Minecraft Loot Table (native loot table system)
            // This uses Minecraft's built-in loot table JSON files (e.g., "minecraft:chests/simple_dungeon")
            if (resolved.definition.lootTable != null && !resolved.definition.lootTable.isEmpty()) {
                // Use Minecraft's native loot table system
                // For each quantity, we roll the loot table once
                for (int i = 0; i < resolved.quantity; i++) {
                    try {
                        ServerLevel level = player.serverLevel();
                        ResourceLocation lootTableId = ResourceLocation.parse(resolved.definition.lootTable);
                        ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);

                        LootTable lootTable = level.getServer().reloadableRegistries()
                            .getLootTable(lootTableKey);

                        if (lootTable == LootTable.EMPTY) {
                            CobblemonEconomy.LOGGER.warn("Loot table not found: " + resolved.definition.lootTable);
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_error").withStyle(ChatFormatting.RED));
                            continue;
                        }

                        // Build loot parameters with player context
                        // Using GIFT context which requires THIS_ENTITY and ORIGIN
                        LootParams lootParams = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .withLuck(player.getLuck())
                            .create(LootContextParamSets.GIFT);

                        // Generate random loot from the table
                        List<ItemStack> loot = lootTable.getRandomItems(lootParams);

                        if (loot.isEmpty()) {
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_empty").withStyle(ChatFormatting.YELLOW));
                        } else {
                            for (ItemStack lootStack : loot) {
                                if (!player.getInventory().add(lootStack.copy())) {
                                    player.drop(lootStack.copy(), false);
                                }
                                player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_open", lootStack.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                            }
                        }
                    } catch (Exception e) {
                        CobblemonEconomy.LOGGER.error("Failed to generate loot from table: " + resolved.definition.lootTable, e);
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_error").withStyle(ChatFormatting.RED));
                    }
                }
            }
            // Check for Lootbox/DropTable (simple list of item IDs - legacy/simple approach)
            else if (resolved.definition.dropTable != null && !resolved.definition.dropTable.isEmpty()) {
                // For lootboxes, we give ONE random item per purchase count? Or one bulk?
                // Typically lootboxes are opened one by one.
                // If quantity > 1, we should probably give 'quantity' items.
                // But resolved.definition.dropTable means the item IS a lootbox in concept (or rather, buying it gives the drop).

                // Let's iterate for quantity
                for (int i = 0; i < resolved.quantity; i++) {
                    String randomId = resolved.definition.dropTable.get(new Random().nextInt(resolved.definition.dropTable.size()));
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(randomId));
                    stackToGive = new ItemStack(item);
                    // Lootbox drops don't usually inherit the NBT of the "crate" item in config,
                    // but the crate itself isn't given.

                    if (!player.getInventory().add(stackToGive)) {
                        player.drop(stackToGive, false);
                    }
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_open", stackToGive.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }

                // We've handled giving items in the loop.
                // Skip the "else" block logic for standard items.
                // Skip the NBT logic below? The NBT logic applies to the "sold item".
                // If dropTable is present, the "sold item" is the drop.
                // But here we might have multiple drops.

                // NOTE: The original code logic was:
                // stackToGive = new ItemStack(item); (random drop)
                // THEN apply NBT from definition to it.
                // THEN give it.

                // If I have a loop, I should probably apply NBT to each drop?
                // But usually dropTable items are raw. NBT in config is usually for the display item or the fixed item.

                // Let's stick to simple logic: If dropTable, repeat logic quantity times.
                // BUT the code structure needs to support "stackToGive" variable which implies single stack.

                // Refactoring handlePurchase to support quantity properly for lootboxes is tricky without changing structure.
                // Let's assume for LOOTBOXES, quantity applies to the number of pulls.

            } else {
                stackToGive = resolved.templateStack.copy();
                stackToGive.setCount(resolved.quantity);
                CobblemonEconomy.LOGGER.info("Creating item to give: " + resolved.originalId);
                CobblemonEconomy.LOGGER.info("Template components: " + resolved.templateStack.getComponents());
                CobblemonEconomy.LOGGER.info("Copy components: " + stackToGive.getComponents());

                // Apply NBT (CustomData) if present in definition (1.20.5+ way)
                if (resolved.definition.nbt != null && !resolved.definition.nbt.isEmpty()) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(resolved.definition.nbt);
                        stackToGive.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    } catch (Exception e) {
                        CobblemonEconomy.LOGGER.error("Failed to parse NBT for item " + resolved.originalId, e);
                    }
                }

                if (!player.getInventory().add(stackToGive)) {
                    player.drop(stackToGive, false);
                }
            }

            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.purchase_success", resolved.quantity + "x " + resolved.name).withStyle(ChatFormatting.GREEN));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
            logTransaction(player, resolved, isPco, false, resolved.quantity, price);

            // Re-resolve the item for the next purchase
            resolved.resolve(player.registryAccess());
        } else {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.insufficient_balance").withStyle(ChatFormatting.RED));
        }

        PerformanceProfiler.end("shop_purchase", profileStart,
            PerformanceProfiler.format("player", player.getGameProfile().getName()) +
                ", " + PerformanceProfiler.format("shop", shopId) +
                ", " + PerformanceProfiler.format("item", resolved.originalId) +
                ", " + PerformanceProfiler.format("qty", resolved.quantity) +
                ", " + PerformanceProfiler.format("currency", isPco ? "PCO" : "POKE"));
    }

    private static String formatDuration(long millis) {
        long totalMinutes = Math.max(0, millis / 60000L);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return minutes + "m";
    }

    private static void handleSell(ServerPlayer player, ResolvedItem resolved, boolean isPco, boolean sellAll, String shopId) {
        long profileStart = PerformanceProfiler.start();
        // Command items cannot be sold
        if (resolved.isCommand) {
            player.sendSystemMessage(Component.literal("This item cannot be sold").withStyle(ChatFormatting.RED));
            return;
        }

        int totalCount = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, resolved.templateStack)) {
                    totalCount += stack.getCount();
                }
            }

        if (totalCount == 0) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.no_item_to_sell").withStyle(ChatFormatting.RED));
            return;
        }

        if (totalCount < resolved.quantity) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.not_enough_items", resolved.quantity).withStyle(ChatFormatting.RED));
            return;
        }

        int amountToSell = 0;
        if (sellAll) {
            amountToSell = (totalCount / resolved.quantity) * resolved.quantity;
        } else {
            amountToSell = resolved.quantity;
        }

        EconomyManager.PurchaseLimitStatus limitStatus = null;
        if (resolved.definition.sellLimit != null && resolved.definition.sellLimit > 0) {
            limitStatus = CobblemonEconomy.getEconomyManager().getSellLimitStatus(
                    player.getUUID(),
                    shopId,
                    resolved.definition.id,
                    resolved.definition.sellLimit,
                    resolved.definition.sellCooldownMinutes
            );
        }

        if (limitStatus != null && limitStatus.enabled && amountToSell > limitStatus.remaining) {
            long remainingMs = limitStatus.resetAtMillis > 0 ? Math.max(0, limitStatus.resetAtMillis - System.currentTimeMillis()) : 0;
            if (limitStatus.remaining <= 0) {
                MutableComponent message = Component.translatable("cobblemon-economy.shop.limit_reached").withStyle(ChatFormatting.RED);
                if (remainingMs > 0) {
                    message = message.append(Component.literal(" "))
                            .append(Component.translatable("cobblemon-economy.shop.limit_resets_in", formatDuration(remainingMs))
                                    .withStyle(ChatFormatting.GRAY));
                }
                player.sendSystemMessage(message);
            } else {
                MutableComponent message = Component.translatable("cobblemon-economy.shop.limit_remaining", limitStatus.remaining)
                        .withStyle(ChatFormatting.RED);
                if (remainingMs > 0) {
                    message = message.append(Component.literal(" "))
                            .append(Component.translatable("cobblemon-economy.shop.limit_resets_in", formatDuration(remainingMs))
                                    .withStyle(ChatFormatting.GRAY));
                }
                player.sendSystemMessage(message);
            }
            return;
        }

        ItemStack toRemove = resolved.templateStack.copy();
        toRemove.setCount(amountToSell);

        if (!removeItem(player, toRemove)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.no_item_to_sell").withStyle(ChatFormatting.RED));
            return;
        }

        BigDecimal totalPrice = BigDecimal.valueOf(resolved.price).multiply(BigDecimal.valueOf(amountToSell));
        if (isPco) CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), totalPrice);
        else CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), totalPrice);

        if (limitStatus != null && limitStatus.enabled) {
            boolean consumed = CobblemonEconomy.getEconomyManager().consumeSellLimit(
                    player.getUUID(),
                    shopId,
                    resolved.definition.id,
                    amountToSell,
                    resolved.definition.sellLimit,
                    resolved.definition.sellCooldownMinutes
            );
            if (!consumed) {
                if (isPco) {
                    CobblemonEconomy.getEconomyManager().subtractPco(player.getUUID(), totalPrice);
                } else {
                    CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), totalPrice);
                }
                ItemStack refundStack = resolved.templateStack.copy();
                refundStack.setCount(amountToSell);
                if (!player.getInventory().add(refundStack)) {
                    player.drop(refundStack, false);
                }
                player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.limit_reached").withStyle(ChatFormatting.RED));
                return;
            }
        }
        
        player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.sell_success", amountToSell, resolved.name, totalPrice, (isPco ? " PCo" : "₽")).withStyle(ChatFormatting.GREEN));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
        logTransaction(player, resolved, isPco, true, amountToSell, totalPrice);
        
        // Re-resolve the item for the next sale
        resolved.resolve(player.registryAccess());

        PerformanceProfiler.end("shop_sell", profileStart,
            PerformanceProfiler.format("player", player.getGameProfile().getName()) +
                ", " + PerformanceProfiler.format("item", resolved.originalId) +
                ", " + PerformanceProfiler.format("qty", amountToSell) +
                ", " + PerformanceProfiler.format("currency", isPco ? "PCO" : "POKE") +
                ", " + PerformanceProfiler.format("sell_all", sellAll));
    }

    private static boolean removeItem(ServerPlayer player, ItemStack toRemove) {
        int remaining = toRemove.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, toRemove)) {
                int count = stack.getCount();
                int taken = Math.min(count, remaining);
                stack.shrink(taken);
                remaining -= taken;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    private static void logTransaction(ServerPlayer player, ResolvedItem resolved, boolean isPco, boolean isSell, int quantity, BigDecimal totalPrice) {
        try {
            File modDir = CobblemonEconomy.getModDirectory();
            File logFile = new File(modDir, "transactions.log");
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currency = isPco ? "PCo" : "₽";
            String type = isSell ? "SELL" : "PURCHASE";
            String logEntry = String.format("[%s] TYPE: %s | PLAYER: %s (%s) | ITEM: %s (%s) | QTY: %d | TOTAL: %s %s\n", 
                timestamp, type, player.getName().getString(), player.getUUID(), resolved.name, resolved.originalId, quantity, totalPrice, currency);
            
            Files.writeString(logFile.toPath(), logEntry, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to log transaction", e);
        }
    }
}
