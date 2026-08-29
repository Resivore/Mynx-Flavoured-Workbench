package dev.resivore.matchajei.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.resivore.matchajei.MatchaNamespaces;
import dev.resivore.matchajei.data.MatchaDisplayData;
import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class MatchaDataScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("matcha_jei_integration/data");
    private static final String MATCHA_DESCRIPTION_MARKER = "klei's matcha flavoured";
    private static final FileToIdConverter RECIPE_FILES = FileToIdConverter.json("recipe");
    private static final FileToIdConverter TRADE_FILES = FileToIdConverter.registry(Registries.VILLAGER_TRADE);
    private static final FileToIdConverter LOOT_FILES = FileToIdConverter.registry(Registries.LOOT_TABLE);
    private static final Map<MinecraftServer, ServerState> STATES = new WeakHashMap<>();
    private static final Set<String> COUNT_ONLY_LOOT_FUNCTIONS = Set.of(
            "minecraft:set_count",
            "minecraft:limit_count",
            "minecraft:explosion_decay",
            "minecraft:enchanted_count_increase",
            "minecraft:apply_bonus"
    );

    private MatchaDataScanner() {
    }

    public static synchronized void initialize(MinecraftServer server) {
        STATES.computeIfAbsent(server, MatchaDataScanner::initializeState);
    }

    public static synchronized MatchaJeiDataPayload payloadFor(MinecraftServer server) {
        ServerState state = STATES.computeIfAbsent(server, MatchaDataScanner::initializeState);
        if (state.payload == null) {
            state.payload = scan(server, state);
        }
        return state.payload;
    }

    public static synchronized void invalidateReloadable(MinecraftServer server) {
        ServerState state = STATES.get(server);
        if (state != null) {
            state.payload = null;
        }
    }

    public static synchronized void forget(MinecraftServer server) {
        STATES.remove(server);
    }

    private static ServerState initializeState(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Set<String> matchaPackIds = findMatchaPackIds(resources);
        HolderLookup.Provider registries = registryProvider(server);
        Map<Identifier, ProfessionLevel> assignments = loadedTradeAssignments(registries);
        Map<Identifier, ResourceDocument> trades = retainAssignedTrades(
                readDocuments(TRADE_FILES.listMatchingResources(resources), TRADE_FILES, matchaPackIds),
                assignments,
                true
        );
        return new ServerState(trades, assignments, revisionOfGroup(trades));
    }

    private static MatchaJeiDataPayload scan(MinecraftServer server, ServerState state) {
        ResourceManager resources = server.getResourceManager();
        Set<String> matchaPackIds = findMatchaPackIds(resources);

        Map<Identifier, ResourceDocument> recipes = readDocuments(
                RECIPE_FILES.listMatchingResources(resources), RECIPE_FILES, matchaPackIds
        );
        Map<Identifier, ResourceDocument> currentTrades = readDocuments(
                TRADE_FILES.listMatchingResources(resources), TRADE_FILES, matchaPackIds
        );
        state.warnIfTradeResourcesChanged(retainAssignedTrades(currentTrades, state.assignments, false));
        Map<Identifier, ResourceDocument> lootTables = readDocuments(
                LOOT_FILES.listMatchingResources(resources), LOOT_FILES, matchaPackIds
        );
        if (recipes.isEmpty() && state.trades.isEmpty() && lootTables.isEmpty()) {
            return MatchaJeiDataPayload.EMPTY;
        }
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, registryProvider(server));
        Set<String> recipeIdentities = collectRecipeIdentities(recipes.values(), registryOps);

        LinkedHashMap<String, ItemStack> ingredients = new LinkedHashMap<>();
        List<MatchaDisplayData.Trade> tradeDisplays = scanTrades(
                state.trades, state.assignments, recipeIdentities, registryOps, ingredients
        );
        List<MatchaDisplayData.Acquisition> acquisitions = scanLoot(
                lootTables, recipeIdentities, registryOps, ingredients
        );
        String revision = revisionOf(recipes, state.trades, lootTables);
        LOGGER.info(
                "Prepared Matcha non-recipe JEI data {}: {} trades, {} acquisitions, {} exact ingredients",
                revision.substring(0, 12), tradeDisplays.size(), acquisitions.size(), ingredients.size()
        );
        return new MatchaJeiDataPayload(
                revision,
                tradeDisplays,
                acquisitions,
                new ArrayList<>(ingredients.values())
        );
    }

    private static HolderLookup.Provider registryProvider(MinecraftServer server) {
        HolderLookup.Provider base = server.registryAccess();
        HolderLookup.Provider reloadable = server.reloadableRegistries().lookup();
        Set<net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>>> baseKeys =
                base.listRegistryKeys().collect(java.util.stream.Collectors.toSet());
        return HolderLookup.Provider.create(java.util.stream.Stream.concat(
                base.listRegistries(),
                reloadable.listRegistries().filter(registry -> !baseKeys.contains(registry.key()))
        ));
    }

    private static Set<String> findMatchaPackIds(ResourceManager resources) {
        LinkedHashSet<String> metadataMatches = new LinkedHashSet<>();
        resources.listPacks().forEach(pack -> {
            try {
                PackMetadataSection metadata = pack.getMetadataSection(PackMetadataSection.SERVER_TYPE);
                if (metadata != null && metadata.description().getString().toLowerCase(Locale.ROOT)
                        .contains(MATCHA_DESCRIPTION_MARKER)) {
                    metadataMatches.add(pack.packId());
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.debug("Unable to inspect datapack metadata for {}", pack.packId(), exception);
            }
        });

        Set<String> recipeOwners = RECIPE_FILES.listMatchingResources(resources).entrySet().stream()
                .filter(entry -> MatchaNamespaces.contains(RECIPE_FILES.fileToId(entry.getKey()).getNamespace()))
                .map(entry -> entry.getValue().sourcePackId())
                .collect(java.util.stream.Collectors.toSet());
        metadataMatches.retainAll(recipeOwners);
        if (metadataMatches.isEmpty()) {
            LOGGER.info("No loaded Matcha datapack passed the metadata and recipe-namespace fingerprint");
        }
        return Set.copyOf(metadataMatches);
    }

    private static Map<Identifier, ProfessionLevel> loadedTradeAssignments(HolderLookup.Provider registries) {
        Optional<? extends HolderLookup.RegistryLookup<TradeSet>> tradeSets = registries.lookup(Registries.TRADE_SET);
        Optional<? extends HolderLookup.RegistryLookup<VillagerProfession>> professions =
                registries.lookup(Registries.VILLAGER_PROFESSION);
        if (tradeSets.isEmpty() || professions.isEmpty()) {
            LOGGER.warn("The loaded profession/trade-set registries are unavailable; Matcha trade displays are empty");
            return Map.of();
        }

        LinkedHashMap<Identifier, ProfessionLevel> assignments = new LinkedHashMap<>();
        professions.get().listElements()
                .sorted(Comparator.comparing(holder -> holder.key().identifier()))
                .forEach(holder -> holder.value().tradeSetsByLevel().int2ObjectEntrySet().stream()
                        .sorted(Comparator.comparingInt(entry -> entry.getIntKey()))
                        .forEach(entry -> addTradeSetAssignments(
                                tradeSets.get(),
                                entry.getValue(),
                                new ProfessionLevel(holder.key().identifier().getPath(), entry.getIntKey()),
                                assignments
                        )));
        for (ResourceKey<TradeSet> wanderingSet : List.of(
                TradeSets.WANDERING_TRADER_BUYING,
                TradeSets.WANDERING_TRADER_COMMON,
                TradeSets.WANDERING_TRADER_UNCOMMON
        )) {
            addTradeSetAssignments(
                    tradeSets.get(),
                    wanderingSet,
                    new ProfessionLevel("wandering_trader", 0),
                    assignments
            );
        }
        return Map.copyOf(assignments);
    }

    private static void addTradeSetAssignments(
            HolderLookup.RegistryLookup<TradeSet> tradeSets,
            ResourceKey<TradeSet> tradeSetKey,
            ProfessionLevel professionLevel,
            Map<Identifier, ProfessionLevel> assignments
    ) {
        tradeSets.get(tradeSetKey).ifPresent(tradeSet -> tradeSet.value().getTrades().stream()
                .map(holder -> holder.unwrapKey())
                .flatMap(Optional::stream)
                .forEach(tradeKey -> assignments.putIfAbsent(tradeKey.identifier(), professionLevel)));
    }

    private static Map<Identifier, ResourceDocument> retainAssignedTrades(
            Map<Identifier, ResourceDocument> documents,
            Map<Identifier, ProfessionLevel> assignments,
            boolean reportIgnored
    ) {
        LinkedHashMap<Identifier, ResourceDocument> loaded = new LinkedHashMap<>();
        documents.values().stream().sorted(Comparator.comparing(ResourceDocument::id))
                .filter(document -> assignments.containsKey(document.id()))
                .forEach(document -> loaded.put(document.id(), document));
        if (reportIgnored && loaded.size() != documents.size()) {
            LOGGER.warn(
                    "Ignored {} Matcha villager-trade resources not assigned by an authoritative loaded trade set",
                    documents.size() - loaded.size()
            );
        }
        return Map.copyOf(loaded);
    }

    private static Map<Identifier, ResourceDocument> readDocuments(
            Map<Identifier, Resource> files,
            FileToIdConverter converter,
            Set<String> matchaPackIds
    ) {
        LinkedHashMap<Identifier, ResourceDocument> documents = new LinkedHashMap<>();
        files.entrySet().stream()
                .filter(entry -> matchaPackIds.contains(entry.getValue().sourcePackId()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Identifier id = converter.fileToId(entry.getKey());
                    try (BufferedReader reader = entry.getValue().openAsReader()) {
                        JsonElement parsed = JsonParser.parseReader(reader);
                        if (parsed.isJsonObject()) {
                            String raw = parsed.toString();
                            documents.put(id, new ResourceDocument(id, parsed.getAsJsonObject(), raw));
                        }
                    } catch (IOException | RuntimeException exception) {
                        LOGGER.warn("Skipping unreadable loaded Matcha resource {}", id, exception);
                    }
                });
        return Map.copyOf(documents);
    }

    private static Set<String> collectRecipeIdentities(
            Iterable<ResourceDocument> documents,
            RegistryOps<JsonElement> registryOps
    ) {
        LinkedHashSet<String> identities = new LinkedHashSet<>();
        for (ResourceDocument document : documents) {
            parseTemplate(document.json().get("result"), registryOps)
                    .map(StackIdentity::key)
                    .ifPresent(identities::add);
        }
        return Set.copyOf(identities);
    }

    private static List<MatchaDisplayData.Trade> scanTrades(
            Map<Identifier, ResourceDocument> documents,
            Map<Identifier, ProfessionLevel> assignments,
            Set<String> recipeIdentities,
            RegistryOps<JsonElement> registryOps,
            Map<String, ItemStack> ingredients
    ) {
        List<MatchaDisplayData.Trade> displays = new ArrayList<>();
        documents.values().stream().sorted(Comparator.comparing(ResourceDocument::id)).forEach(document -> {
            JsonObject json = document.json();
            if (hasNonEmptyArray(json, "given_item_modifiers")) {
                return;
            }
            Optional<StackIdentity> first = parseTemplate(json.get("wants"), registryOps);
            Optional<StackIdentity> second = parseTemplate(json.get("additional_wants"), registryOps);
            Optional<StackIdentity> output = parseTemplate(json.get("gives"), registryOps);
            if (first.isEmpty() || output.isEmpty()
                    || (json.has("additional_wants") && second.isEmpty())) {
                return;
            }
            first.filter(identity -> isComponentStack(identity.stack()))
                    .filter(identity -> isNonRecipeIdentity(identity, recipeIdentities))
                    .ifPresent(identity -> addIngredient(ingredients, identity));
            second.filter(identity -> isComponentStack(identity.stack()))
                    .filter(identity -> isNonRecipeIdentity(identity, recipeIdentities))
                    .ifPresent(identity -> addIngredient(ingredients, identity));
            output.filter(identity -> isComponentStack(identity.stack()))
                    .filter(identity -> isNonRecipeIdentity(identity, recipeIdentities))
                    .ifPresent(identity -> addIngredient(ingredients, identity));

            ItemStack secondStack = second.map(StackIdentity::stack).orElse(ItemStack.EMPTY);
            if (!isComponentStack(first.get().stack())
                    && !isComponentStack(secondStack)
                    && !isComponentStack(output.get().stack())) {
                return;
            }
            ProfessionLevel professionLevel = assignments.get(document.id());
            if (professionLevel == null) {
                return;
            }
            String displayKey = shortHash(
                    document.id() + "\n" + first.get().key() + "\n"
                            + second.map(StackIdentity::key).orElse("") + "\n" + output.get().key()
            );
            displays.add(new MatchaDisplayData.Trade(
                    document.id(),
                    displayKey,
                    professionLevel.profession(),
                    professionLevel.level(),
                    first.get().stack(),
                    secondStack,
                    output.get().stack(),
                    json.has("merchant_predicate")
            ));
        });
        return List.copyOf(displays);
    }

    private static List<MatchaDisplayData.Acquisition> scanLoot(
            Map<Identifier, ResourceDocument> documents,
            Set<String> recipeIdentities,
            RegistryOps<JsonElement> registryOps,
            Map<String, ItemStack> ingredients
    ) {
        Map<Identifier, LootNode> nodes = new LinkedHashMap<>();
        Set<Identifier> referenced = new HashSet<>();
        documents.values().stream().sorted(Comparator.comparing(ResourceDocument::id)).forEach(document -> {
            LootNode node = parseLootNode(document.json(), registryOps);
            nodes.put(document.id(), node);
            referenced.addAll(node.allReferences());
        });

        List<MatchaDisplayData.Acquisition> displays = new ArrayList<>();
        nodes.keySet().stream()
                .filter(id -> isAcquisitionSource(id, referenced))
                .sorted()
                .forEach(source -> resolveLoot(source, nodes, new HashSet<>()).values().stream()
                        .filter(identity -> isNonRecipeIdentity(identity, recipeIdentities))
                        .sorted(Comparator.comparing(StackIdentity::key))
                        .forEach(identity -> {
                            addIngredient(ingredients, identity);
                            displays.add(new MatchaDisplayData.Acquisition(
                                    source,
                                    shortHash(source + "\n" + identity.key()),
                                    acquisitionDescription(source, identity.durabilityVaries()),
                                    identity.stack()
                            ));
                        }));
        return List.copyOf(displays);
    }

    private static boolean isAcquisitionSource(Identifier id, Set<Identifier> referenced) {
        String path = id.getPath();
        if (Set.of(
                "gameplay/chicken_lay",
                "gameplay/fishing",
                "gameplay/piglin_bartering",
                "gameplay/turtle_grow"
        ).contains(path)) {
            return true;
        }
        if (referenced.contains(id)) {
            return false;
        }
        if (path.startsWith("chests/equipment/") || path.equals("chests/fishing_buried_treasure")) {
            return false;
        }
        return path.startsWith("chests/")
                || path.startsWith("archaeology/")
                || path.startsWith("entities/")
                || path.startsWith("blocks/")
                || path.startsWith("harvest/")
                || path.startsWith("pots/")
                || path.startsWith("spawners/")
                || path.startsWith("shearing/");
    }

    private static LootNode parseLootNode(JsonObject table, RegistryOps<JsonElement> registryOps) {
        LinkedHashMap<String, StackIdentity> items = new LinkedHashMap<>();
        LinkedHashSet<Identifier> children = new LinkedHashSet<>();
        LinkedHashSet<Identifier> allReferences = new LinkedHashSet<>();
        collectLootTableReferences(table, allReferences);
        List<JsonObject> tableFunctions = functionObjects(table.get("functions"));
        JsonElement poolsElement = table.get("pools");
        if (poolsElement instanceof JsonArray pools) {
            for (JsonElement poolElement : pools) {
                if (!poolElement.isJsonObject()) {
                    continue;
                }
                JsonObject pool = poolElement.getAsJsonObject();
                List<JsonObject> inherited = combine(tableFunctions, functionObjects(pool.get("functions")));
                JsonElement entriesElement = pool.get("entries");
                if (entriesElement instanceof JsonArray entries) {
                    for (JsonElement entry : entries) {
                        walkLootEntry(entry, inherited, registryOps, items, children);
                    }
                }
            }
        }
        return new LootNode(Map.copyOf(items), Set.copyOf(children), Set.copyOf(allReferences));
    }

    private static void collectLootTableReferences(JsonElement element, Set<Identifier> references) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectLootTableReferences(child, references));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (stringValue(object, "type").filter("minecraft:loot_table"::equals).isPresent()) {
            stringValue(object, "value").map(Identifier::tryParse).ifPresent(id -> {
                if (id != null) {
                    references.add(id);
                }
            });
        }
        object.entrySet().forEach(entry -> collectLootTableReferences(entry.getValue(), references));
    }

    private static void walkLootEntry(
            JsonElement element,
            List<JsonObject> inheritedFunctions,
            RegistryOps<JsonElement> registryOps,
            Map<String, StackIdentity> items,
            Set<Identifier> children
    ) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject entry = element.getAsJsonObject();
        List<JsonObject> functions = combine(inheritedFunctions, functionObjects(entry.get("functions")));
        String type = stringValue(entry, "type").orElse("");
        if (type.equals("minecraft:item")) {
            parseLootItem(entry, functions, registryOps).ifPresent(identity ->
                    items.merge(identity.key(), identity, StackIdentity::combine)
            );
        } else if (type.equals("minecraft:loot_table") && functionsArePassThrough(functions)) {
            stringValue(entry, "value").map(Identifier::tryParse).ifPresent(id -> {
                if (id != null) {
                    children.add(id);
                }
            });
        }
        for (String childField : List.of("children", "entries")) {
            JsonElement childrenElement = entry.get(childField);
            if (childrenElement instanceof JsonArray childEntries) {
                childEntries.forEach(child -> walkLootEntry(child, functions, registryOps, items, children));
            }
        }
    }

    private static Optional<StackIdentity> parseLootItem(
            JsonObject entry,
            List<JsonObject> functions,
            RegistryOps<JsonElement> registryOps
    ) {
        Optional<String> itemId = stringValue(entry, "name");
        if (itemId.isEmpty()) {
            return Optional.empty();
        }
        boolean durabilityVaries = functions.stream()
                .anyMatch(function -> stringValue(function, "function")
                        .filter("minecraft:set_damage"::equals).isPresent());
        JsonObject components = new JsonObject();
        for (JsonObject function : functions) {
            String functionId = stringValue(function, "function").orElse("");
            if (functionId.equals("minecraft:set_components")) {
                JsonElement patch = function.get("components");
                if (!(patch instanceof JsonObject patchObject)) {
                    return Optional.empty();
                }
                patchObject.entrySet().forEach(component -> components.add(component.getKey(), component.getValue()));
            } else if (functionId.equals("minecraft:set_name")) {
                if (!applyStaticName(function, components)) {
                    return Optional.empty();
                }
            } else if (functionId.equals("minecraft:set_damage")) {
                // Durability is transient in JEI identity but qualified in the acquisition text.
            } else if (!COUNT_ONLY_LOOT_FUNCTIONS.contains(functionId)) {
                return Optional.empty();
            }
        }
        if (components.isEmpty()) {
            return Optional.empty();
        }
        JsonObject template = new JsonObject();
        template.addProperty("id", itemId.get());
        template.addProperty("count", 1);
        template.add("components", components);
        return parseTemplate(template, registryOps)
                .map(identity -> identity.withDurabilityVaries(durabilityVaries));
    }

    private static boolean applyStaticName(JsonObject function, JsonObject components) {
        if (hasNonEmptyArray(function, "conditions")) {
            return false;
        }
        JsonElement name = function.get("name");
        if (name == null || !name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) {
            return false;
        }
        String target = stringValue(function, "target").orElse("custom_name");
        String componentId = switch (target) {
            case "item_name" -> "minecraft:item_name";
            case "custom_name" -> "minecraft:custom_name";
            default -> null;
        };
        if (componentId == null) {
            return false;
        }
        components.add(componentId, name);
        return true;
    }

    private static boolean functionsArePassThrough(List<JsonObject> functions) {
        return functions.stream()
                .map(function -> stringValue(function, "function").orElse(""))
                .allMatch(COUNT_ONLY_LOOT_FUNCTIONS::contains);
    }

    private static Map<String, StackIdentity> resolveLoot(
            Identifier table,
            Map<Identifier, LootNode> nodes,
            Set<Identifier> visiting
    ) {
        LootNode node = nodes.get(table);
        if (node == null || !visiting.add(table)) {
            return Map.of();
        }
        LinkedHashMap<String, StackIdentity> resolved = new LinkedHashMap<>(node.directItems());
        node.children().stream().sorted().forEach(child ->
                resolveLoot(child, nodes, visiting).forEach((key, identity) ->
                        resolved.merge(key, identity, StackIdentity::combine)
                )
        );
        visiting.remove(table);
        return Map.copyOf(resolved);
    }

    private static Optional<StackIdentity> parseTemplate(
            JsonElement element,
            RegistryOps<JsonElement> registryOps
    ) {
        if (!(element instanceof JsonObject object)) {
            return Optional.empty();
        }
        JsonElement count = object.get("count");
        if (count != null && (!count.isJsonPrimitive() || !count.getAsJsonPrimitive().isNumber())) {
            return Optional.empty();
        }
        return ItemStackTemplate.CODEC.parse(registryOps, object)
                .resultOrPartial(message -> LOGGER.debug("Skipping unsupported exact stack template: {}", message))
                .map(ItemStackTemplate::create)
                .filter(stack -> !stack.isEmpty())
                .flatMap(stack -> canonicalStack(stack, registryOps).map(key -> new StackIdentity(key, stack)));
    }

    private static Optional<String> canonicalStack(ItemStack stack, RegistryOps<JsonElement> registryOps) {
        ItemStack normalized = stack.copyWithCount(1);
        return ItemStack.CODEC.encodeStart(registryOps, normalized)
                .resultOrPartial(message -> LOGGER.debug("Unable to canonicalize exact stack: {}", message))
                .map(JsonElement::toString);
    }

    private static void addIngredient(Map<String, ItemStack> ingredients, StackIdentity identity) {
        ingredients.putIfAbsent(identity.key(), identity.stack().copyWithCount(1));
    }

    private static boolean isComponentStack(ItemStack stack) {
        return !stack.isEmpty() && !stack.getComponentsPatch().isEmpty();
    }

    private static boolean isNonRecipeIdentity(StackIdentity identity, Set<String> recipeIdentities) {
        return !recipeIdentities.contains(identity.key());
    }

    private static String acquisitionDescription(Identifier source, boolean durabilityVaries) {
        String path = source.getPath();
        String provenance;
        if (path.equals("gameplay/fishing")) {
            provenance = "fishing loot";
        } else if (path.startsWith("chests/")) {
            provenance = title(path.substring("chests/".length())) + " chest loot";
        } else if (path.startsWith("archaeology/")) {
            provenance = title(path.substring("archaeology/".length())) + " archaeology loot";
        } else if (path.startsWith("entities/")) {
            provenance = title(path.substring("entities/".length())) + " drops";
        } else if (path.startsWith("blocks/")) {
            provenance = title(path.substring("blocks/".length())) + " block drops";
        } else {
            provenance = title(path) + " loot";
        }
        String qualification = durabilityVaries
                ? "; displayed at full durability, but acquired durability, chance, and conditions vary"
                : "; chance and conditions vary";
        return "Possible " + provenance + qualification;
    }

    private static String title(String path) {
        String[] words = path.replace('/', ' ').replace('_', ' ').split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return result.toString();
    }

    private static boolean hasNonEmptyArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value instanceof JsonArray array && !array.isEmpty();
    }

    private static Optional<String> stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return Optional.of(value.getAsString());
        }
        return Optional.empty();
    }

    private static List<JsonObject> functionObjects(JsonElement element) {
        if (!(element instanceof JsonArray array)) {
            return List.of();
        }
        List<JsonObject> functions = new ArrayList<>();
        array.forEach(value -> {
            if (value.isJsonObject()) {
                functions.add(value.getAsJsonObject());
            }
        });
        return List.copyOf(functions);
    }

    private static List<JsonObject> combine(List<JsonObject> first, List<JsonObject> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        List<JsonObject> combined = new ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return List.copyOf(combined);
    }

    private static String revisionOf(
            Map<Identifier, ResourceDocument> recipes,
            Map<Identifier, ResourceDocument> trades,
            Map<Identifier, ResourceDocument> lootTables
    ) {
        MessageDigest digest = sha256();
        List<Map<Identifier, ResourceDocument>> groups = List.of(recipes, trades, lootTables);
        for (Map<Identifier, ResourceDocument> group : groups) {
            group.values().stream().sorted(Comparator.comparing(ResourceDocument::id)).forEach(document -> {
                digest.update(document.id().toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(document.raw().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            });
        }
        return hex(digest.digest());
    }

    private static String revisionOfGroup(Map<Identifier, ResourceDocument> documents) {
        MessageDigest digest = sha256();
        documents.values().stream().sorted(Comparator.comparing(ResourceDocument::id)).forEach(document -> {
            digest.update(document.id().toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(document.raw().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
        });
        return hex(digest.digest());
    }

    private static String shortHash(String value) {
        MessageDigest digest = sha256();
        return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String hex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private record ResourceDocument(Identifier id, JsonObject json, String raw) {
    }

    private record StackIdentity(String key, ItemStack stack, boolean durabilityVaries) {
        private StackIdentity(String key, ItemStack stack) {
            this(key, stack, false);
        }

        private StackIdentity withDurabilityVaries(boolean varies) {
            boolean combined = durabilityVaries || varies;
            return combined == durabilityVaries ? this : new StackIdentity(key, stack, combined);
        }

        private StackIdentity combine(StackIdentity other) {
            return new StackIdentity(key, stack, durabilityVaries || other.durabilityVaries);
        }
    }

    private record LootNode(
            Map<String, StackIdentity> directItems,
            Set<Identifier> children,
            Set<Identifier> allReferences
    ) {
    }

    private record ProfessionLevel(String profession, int level) {
    }

    private static final class ServerState {
        private final Map<Identifier, ResourceDocument> trades;
        private final Map<Identifier, ProfessionLevel> assignments;
        private final String tradeRevision;
        private String lastWarnedTradeRevision = "";
        private MatchaJeiDataPayload payload;

        private ServerState(
                Map<Identifier, ResourceDocument> trades,
                Map<Identifier, ProfessionLevel> assignments,
                String tradeRevision
        ) {
            this.trades = trades;
            this.assignments = assignments;
            this.tradeRevision = tradeRevision;
        }

        private void warnIfTradeResourcesChanged(Map<Identifier, ResourceDocument> currentTrades) {
            String currentRevision = revisionOfGroup(currentTrades);
            if (!currentRevision.equals(tradeRevision) && !currentRevision.equals(lastWarnedTradeRevision)) {
                LOGGER.warn(
                        "Loaded Matcha villager-trade resources changed during /reload; "
                                + "the authoritative startup registry snapshot remains active until server restart"
                );
                lastWarnedTradeRevision = currentRevision;
            }
        }
    }
}
