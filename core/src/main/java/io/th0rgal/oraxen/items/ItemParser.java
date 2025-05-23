package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.hook.PackGeneratorPluginHook;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import ltd.lemongaming.packgenerator.annotation.CustomModel;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import net.Indyuce.mmoitems.MMOItems;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.tag.DamageTypeTags;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class ItemParser {

    public static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private final OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private final Material type;
    private WrappedMMOItem mmoItem;
    private WrappedCrucibleItem crucibleItem;
    private WrappedEcoItem ecoItem;
    private ItemParser templateItem;
    private boolean configUpdated = false;

    public ItemParser(ConfigurationSection section) {
        this.section = section;

        if (section.isString("template"))
            templateItem = ItemTemplate.getParserTemplate(section.getString("template"));

        ConfigurationSection crucibleSection = section.getConfigurationSection("crucible");
        ConfigurationSection mmoSection = section.getConfigurationSection("mmoitem");
        ConfigurationSection ecoItemSection = section.getConfigurationSection("ecoitem");
        if (crucibleSection != null)
            crucibleItem = new WrappedCrucibleItem(crucibleSection);
        else if (section.isString("crucible_id"))
            crucibleItem = new WrappedCrucibleItem(section.getString("crucible_id"));
        else if (ecoItemSection != null)
            ecoItem = new WrappedEcoItem(ecoItemSection);
        else if (section.isString("ecoitem_id"))
            ecoItem = new WrappedEcoItem(section.getString("ecoitem_id"));
        else if (mmoSection != null)
            mmoItem = new WrappedMMOItem(mmoSection);

        String resourceName = section.getString("resource", "");
        CustomModel resource = PackGeneratorPluginHook.getCustomModel(resourceName).orElse(null);
        if (resource != null) {
            Logs.logInfo("Successfully found a PackGenerator resource '%s' for item: %s".formatted(resource.name(), section.getName()));
        }
        Material material = resource != null ? Material.matchMaterial(resource.material().name()) : Material.getMaterial(section.getString("material", ""));
        if (material == null) material = usesTemplate() ? templateItem.type : Material.PAPER;
        type = material;

        oraxenMeta = new OraxenMeta();

        if (resource != null) {
            MODEL_DATAS_BY_ID.put(section.getName(),
                new ModelData(type, resource.getModel(), resource.id()));
            return;
        }

        if (section.isConfigurationSection("Pack")) {
            ConfigurationSection packSection = section.getConfigurationSection("Pack");
            oraxenMeta.setPackInfos(packSection);
            assert packSection != null;
            if (packSection.isInt("custom_model_data"))
                MODEL_DATAS_BY_ID.put(section.getName(),
                        new ModelData(type, oraxenMeta.getModelName(), packSection.getInt("custom_model_data")));
        }
    }

    public boolean usesMMOItems() {
        return crucibleItem == null && ecoItem == null && mmoItem != null && mmoItem.build() != null;
    }

    public boolean usesCrucibleItems() {
        return mmoItem == null && ecoItem == null && crucibleItem != null && crucibleItem.build() != null;
    }

    public boolean usesEcoItems() {
        return mmoItem == null && crucibleItem == null && ecoItem != null && ecoItem.build() != null;
    }

    public boolean usesTemplate() {
        return templateItem != null;
    }

    public ItemBuilder buildItem() {
        ItemBuilder item;

        if (usesCrucibleItems())
            item = new ItemBuilder(crucibleItem);
        else if (usesMMOItems())
            item = new ItemBuilder(mmoItem);
        else if (usesEcoItems())
            item = new ItemBuilder(ecoItem);
        else
            item = new ItemBuilder(type);

        // If item has a template, apply the template ontop of the builder made above
        return applyConfig(usesTemplate() ? templateItem.applyConfig(item) : item);
    }

    private ItemBuilder applyConfig(ItemBuilder item) {
        try {
            if (section.contains("displayname")) {
                if (VersionUtil.atOrAbove("1.20.5"))
                    configUpdated = true;
                else
                    item.setDisplayName(section.getString("displayname", ""));
            }

            if (section.contains("customname")) {
                if (!VersionUtil.atOrAbove("1.20.5"))
                    configUpdated = true;
                else
                    item.setDisplayName(section.getString("customname", ""));
            }

            // if (section.contains("type"))
            // item.setType(Material.getMaterial(section.getString("type", "PAPER")));
            if (section.contains("lore"))
                item.setLore(section.getStringList("lore").stream().map(AdventureUtils::parseMiniMessage).toList());
            if (section.contains("unbreakable"))
                item.setUnbreakable(section.getBoolean("unbreakable", false));
            if (section.contains("unstackable"))
                item.setUnstackable(section.getBoolean("unstackable", false));
            if (section.contains("color"))
                item.setColor(Utils.toColor(section.getString("color", "#FFFFFF")));
            if (section.contains("trim_pattern"))
                item.setTrimPattern(Key.key(section.getString("trim_pattern", "")));

            parseDataComponents(item);
            parseMiscOptions(item);
            parseVanillaSections(item);
            parseOraxenSections(item);
            item.setOraxenMeta(oraxenMeta);
            return item;
        } catch (Exception e) {
            String itemId = section != null ? section.getName() : "unknown";
            Logs.logError("Error building item \"" + itemId + "\"");
            Logs.logError(e.getMessage());
            if (Settings.DEBUG.toBool()) {
                e.printStackTrace();
            }
            // Still return the item to avoid NPE, even if it's not fully configured
            return item;
        }
    }

    private void parseDataComponents(ItemBuilder item) {
        ConfigurationSection section = mergeWithTemplateSection();
        if (section.contains("itemname") && VersionUtil.atOrAbove("1.20.5"))
            item.setItemName(section.getString("itemname"));
        else if (section.contains("displayname"))
            item.setItemName(section.getString("displayname"));

        ConfigurationSection components = section.getConfigurationSection("Components");
        if (components == null || !VersionUtil.atOrAbove("1.20.5"))
            return;

        // Handle legacy components for backward compatibility
        handleLegacyComponents(item, components);

        // Handle generic components
        if (VersionUtil.atOrAbove("1.21.3")) {
            for (String key : components.getKeys(false)) {
                // Skip legacy components that are handled separately
                if (isLegacyComponent(key))
                    continue;

                Object value = components.get(key);
                if (value instanceof ConfigurationSection || value instanceof Map) {
                    NMSHandlers.getHandler().setComponent(item, key, value);
                }
            }
        }
    }

    private void handleLegacyComponents(ItemBuilder item, ConfigurationSection components) {

        if (components.contains("durability")) {
            item.setDamagedOnBlockBreak(components.getBoolean("durability.damage_block_break"));
            item.setDamagedOnEntityHit(components.getBoolean("durability.damage_entity_hit"));
            item.setDurability(Math.max(components.getInt("durability.value"), components.getInt("durability", 1)));
        }
        if (components.contains("fire_resistant"))
            item.setFireResistant(components.getBoolean("fire_resistant"));
        if (components.contains("hide_tooltip"))
            item.setHideToolTip(components.getBoolean("hide_tooltip"));

        NMSHandler nmsHandler = NMSHandlers.getHandler();
        if (nmsHandler == null) {
            Logs.logWarning("NMSHandler is null - some components won't work properly");
            if (Settings.DEBUG.toBool()) {
                Logs.logError("Item parsing: " + (section != null ? section.getName() : "unknown section"));
                new Exception("NMSHandler is null").printStackTrace();
            }
        } else {
            Optional.ofNullable(components.getConfigurationSection("food"))
                    .ifPresent(food -> nmsHandler.foodComponent(item, food));
        }

        Optional.ofNullable(components.getConfigurationSection("tool"))
                .ifPresent(toolSection -> parseToolComponent(item, toolSection));

        if (!VersionUtil.atOrAbove("1.21"))
            return;

        ConfigurationSection jukeboxSection = components.getConfigurationSection("jukebox_playable");
        if (jukeboxSection != null && VersionUtil.isPaperServer()) {
            try {
                JukeboxPlayableComponent jukeboxPlayable = new ItemStack(Material.MUSIC_DISC_CREATOR).getItemMeta()
                        .getJukeboxPlayable();

                try {
                    jukeboxPlayable.setShowInTooltip(jukeboxSection.getBoolean("show_in_tooltip"));
                } catch (NoSuchMethodError e) {
                    Logs.logWarning(
                            "Error setting jukebox show_in_tooltip: This method is not available in your server version");
                    if (Settings.DEBUG.toBool()) {
                        e.printStackTrace();
                    }
                }

                try {
                    jukeboxPlayable.setSongKey(NamespacedKey.fromString(jukeboxSection.getString("song_key", "")));
                } catch (NoSuchMethodError e) {
                    Logs.logWarning(
                            "Error setting jukebox song_key: This method is not available in your server version");
                    if (Settings.DEBUG.toBool()) {
                        e.printStackTrace();
                    }
                }

                item.setJukeboxPlayable(jukeboxPlayable);
            } catch (Exception e) {
                Logs.logWarning("Failed to create JukeboxPlayableComponent for item: " + section.getName());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        } else if (jukeboxSection != null) {
            Logs.logInfo("JukeboxPlayableComponent is only supported on Paper servers. Skipping this component.");
        }

        if (!VersionUtil.atOrAbove("1.21.2"))
            return;
        Optional.ofNullable(components.getConfigurationSection("equippable"))
                .ifPresent(equippable -> parseEquippableComponent(item, equippable));

        Optional.ofNullable(components.getConfigurationSection("use_cooldown")).ifPresent((cooldownSection) -> {
            try {
                UseCooldownComponent useCooldownComponent = new ItemStack(Material.PAPER).getItemMeta()
                        .getUseCooldown();
                String group = Optional.ofNullable(cooldownSection.getString("group"))
                        .orElse("oraxen:" + OraxenItems.getIdByItem(item));
                if (!group.isEmpty())
                    useCooldownComponent.setCooldownGroup(NamespacedKey.fromString(group));
                useCooldownComponent
                        .setCooldownSeconds((float) Math.max(cooldownSection.getDouble("seconds", 1.0), 0f));
                item.setUseCooldownComponent(useCooldownComponent);
            } catch (NoSuchMethodError | Exception e) {
                Logs.logWarning(
                        "Error setting UseCooldownComponent: This component is not available in your server version");
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        });

        Optional.ofNullable(components.getConfigurationSection("use_remainder"))
                .ifPresent(useRemainder -> parseUseRemainderComponent(item, useRemainder));

        Optional.ofNullable(components.getString("tooltip_style")).map(NamespacedKey::fromString)
                .ifPresent(item::setTooltipStyle);
        Optional.ofNullable(components.getString("item_model")).map(NamespacedKey::fromString)
                .ifPresent(item::setItemModel);

        if (nmsHandler != null) {
            Optional.ofNullable(components.getConfigurationSection("consumable"))
                    .ifPresent(consumableSection -> nmsHandler.consumableComponent(item, consumableSection));
        }
    }

    private boolean isLegacyComponent(String key) {
        return key.equals("durability") ||
                key.equals("fire_resistant") ||
                key.equals("hide_tooltip") ||
                key.equals("food") ||
                key.equals("tool") ||
                key.equals("jukebox_playable") ||
                key.equals("equippable") ||
                key.equals("use_cooldown") ||
                key.equals("use_remainder") ||
                key.equals("tooltip_style") ||
                key.equals("item_model") ||
                key.equals("consumable");
    }

    private void parseUseRemainderComponent(ItemBuilder item, @NotNull ConfigurationSection useRemainderSection) {
        ItemStack result;
        int amount = useRemainderSection.getInt("amount", 1);

        if (useRemainderSection.contains("oraxen_item"))
            result = ItemUpdater
                    .updateItem(OraxenItems.getItemById(useRemainderSection.getString("oraxen_item")).build());
        else if (useRemainderSection.contains("crucible_item"))
            result = new WrappedCrucibleItem(useRemainderSection.getString("crucible_item")).build();
        else if (useRemainderSection.contains("mmoitems_id") && useRemainderSection.isString("mmoitems_type"))
            result = MMOItems.plugin.getItem(useRemainderSection.getString("mmoitems_type"),
                    useRemainderSection.getString("mmoitems_id"));
        else if (useRemainderSection.contains("ecoitem_id"))
            result = new WrappedEcoItem(useRemainderSection.getString("ecoitem_id")).build();
        else if (useRemainderSection.contains("minecraft_type")) {
            Material material = Material.getMaterial(useRemainderSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir())
                return;
            result = new ItemStack(material);
        } else
            result = useRemainderSection.getItemStack("minecraft_item");

        if (result != null)
            result.setAmount(amount);
        item.setUseRemainder(result);
    }

    @SuppressWarnings({ "UnstableApiUsage", "unchecked" })
    private void parseToolComponent(ItemBuilder item, @NotNull ConfigurationSection toolSection) {
        ToolComponent toolComponent = new ItemStack(type).getItemMeta().getTool();
        toolComponent.setDamagePerBlock(Math.max(toolSection.getInt("damage_per_block", 1), 0));
        toolComponent.setDefaultMiningSpeed(Math.max((float) toolSection.getDouble("default_mining_speed", 1.0), 0f));

        for (Map<?, ?> ruleEntry : toolSection.getMapList("rules")) {
            float speed = NumberUtils.toFloat(String.valueOf(ruleEntry.get("speed")), 1f);
            boolean correctForDrops = Boolean.parseBoolean(String.valueOf(ruleEntry.get("correct_for_drops")));
            Set<Material> materials = new HashSet<>();
            Set<Tag<Material>> tags = new HashSet<>();

            if (ruleEntry.containsKey("material")) {
                try {
                    Material material = Material.valueOf(String.valueOf(ruleEntry.get("material")));
                    if (material.isBlock())
                        materials.add(material);
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("materials")) {
                try {
                    List<String> materialIds = (List<String>) ruleEntry.get("materials");
                    for (String materialId : materialIds) {
                        Material material = Material.valueOf(materialId);
                        if (material.isBlock())
                            materials.add(material);
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"materials\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tag")) {
                try {
                    NamespacedKey tagKey = NamespacedKey.fromString(String.valueOf(ruleEntry.get("tag")));
                    if (tagKey != null)
                        tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"tag\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tags")) {
                try {
                    for (String tagString : (List<String>) ruleEntry.get("tags")) {
                        NamespacedKey tagKey = NamespacedKey.fromString(tagString);
                        if (tagKey != null)
                            tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (!materials.isEmpty())
                toolComponent.addRule(materials, speed, correctForDrops);
            for (Tag<Material> tag : tags)
                toolComponent.addRule(tag, speed, correctForDrops);
        }

        item.setToolComponent(toolComponent);
    }

    private void parseEquippableComponent(ItemBuilder item, ConfigurationSection equippableSection) {
        EquippableComponent equippableComponent = new ItemStack(type).getItemMeta().getEquippable();

        String slot = equippableSection.getString("slot");
        try {
            equippableComponent.setSlot(EquipmentSlot.valueOf(slot));
        } catch (Exception e) {
            Logs.logWarning("Error parsing equippable-component in %s...".formatted(section.getName()));
            Logs.logWarning("Invalid \"slot\"-value %s".formatted(slot));
            Logs.logWarning("Valid values are: %s".formatted(StringUtils.join(EquipmentSlot.values())));
            return;
        }

        List<EntityType> entityTypes = equippableSection.getStringList("allowed_entity_types").stream()
                .map(e -> EnumUtils.getEnum(EntityType.class, e)).toList();
        if (equippableSection.contains("allowed_entity_types"))
            equippableComponent.setAllowedEntities(entityTypes.isEmpty() ? null : entityTypes);
        if (equippableSection.contains("damage_on_hurt"))
            equippableComponent.setDamageOnHurt(equippableSection.getBoolean("damage_on_hurt", true));
        if (equippableSection.contains("dispensable"))
            equippableComponent.setDispensable(equippableSection.getBoolean("dispensable", true));
        if (equippableSection.contains("swappable"))
            equippableComponent.setSwappable(equippableSection.getBoolean("swappable", true));

        Optional.ofNullable(equippableSection.getString("model", null)).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setModel);
        Optional.ofNullable(equippableSection.getString("camera_overlay")).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setCameraOverlay);

        // Only use Registry.SOUNDS::get if we're running on Paper
        if (VersionUtil.isPaperServer() && equippableSection.contains("equip_sound")) {
            try {
                Optional.ofNullable(equippableSection.getString("equip_sound"))
                        .map(Key::key)
                        .map(key -> org.bukkit.Registry.SOUNDS.get(key))
                        .ifPresent(equippableComponent::setEquipSound);
            } catch (NoSuchMethodError e) {
                // This will catch errors on older server versions
                Logs.logWarning("Error setting equip_sound: Your server version doesn't support this feature.");
            }
        }

        item.setEquippableComponent(equippableComponent);
    }

    private void parseMiscOptions(ItemBuilder item) {
        if (section.getBoolean("injectId", true))
            item.setCustomTag(OraxenItems.ITEM_ID, PersistentDataType.STRING, section.getName());

        ConfigurationSection section = mergeWithTemplateSection();
        oraxenMeta.setNoUpdate(section.getBoolean("no_auto_update", false));
        oraxenMeta.setDisableEnchanting(section.getBoolean("disable_enchanting", false));
        oraxenMeta.setExcludedFromInventory(section.getBoolean("excludeFromInventory", false));
        oraxenMeta.setExcludedFromCommands(section.getBoolean("excludeFromCommands", false));
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void parseVanillaSections(ItemBuilder item) {
        ConfigurationSection section = mergeWithTemplateSection();
        if (section.contains("ItemFlags")) {
            List<String> itemFlags = section.getStringList("ItemFlags");
            for (String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }

        if (section.contains("PotionEffects")) {
            List<LinkedHashMap<String, Object>> potionEffects = (List<LinkedHashMap<String, Object>>) section
                    .getList("PotionEffects");
            if (potionEffects == null)
                return;
            for (Map<String, Object> serializedPotionEffect : potionEffects) {
                PotionEffectType effect = PotionUtils
                        .getEffectType((String) serializedPotionEffect.getOrDefault("type", ""));
                if (effect == null)
                    return;
                int duration = (int) serializedPotionEffect.getOrDefault("duration", 60);
                int amplifier = (int) serializedPotionEffect.getOrDefault("amplifier", 0);
                boolean ambient = (boolean) serializedPotionEffect.getOrDefault("ambient", true);
                boolean particles = (boolean) serializedPotionEffect.getOrDefault("particles", true);
                boolean icon = (boolean) serializedPotionEffect.getOrDefault("icon", true);
                item.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles, icon));
            }
        }

        if (section.contains("PersistentData")) {
            try {
                List<LinkedHashMap<String, Object>> dataHolder = (List<LinkedHashMap<String, Object>>) section
                        .getList("PersistentData");
                for (LinkedHashMap<String, Object> attributeJson : dataHolder) {
                    String[] keyContent = ((String) attributeJson.get("key")).split(":");
                    final Object persistentDataType = PersistentDataType.class
                            .getDeclaredField((String) attributeJson.get("type")).get(null);
                    item.addCustomTag(new NamespacedKey(keyContent[0], keyContent[1]),
                            (PersistentDataType<Object, Object>) persistentDataType,
                            attributeJson.get("value"));
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        List<Map<String, Object>> attributes = (List<Map<String, Object>>) section.getList("AttributeModifiers");
        if (attributes != null) {
            for (Map<String, Object> attributeJson : attributes) {
                try {
                    attributeJson.putIfAbsent("uuid", UUID.randomUUID().toString());
                    attributeJson.putIfAbsent("name", "oraxen:modifier");
                    attributeJson.putIfAbsent("key", "oraxen:modifier");

                    AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                    Attribute attribute = AttributeWrapper.fromString((String) attributeJson.get("attribute"));

                    if (attribute != null) {
                        item.addAttributeModifiers(attribute, attributeModifier);
                    } else {
                        Logs.logWarning("Attribute not found for key: " + attributeJson.get("attribute") + " in item: "
                                + section.getName());
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing AttributeModifiers in " + section.getName());
                    if (Settings.DEBUG.toBool()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (section.contains("Enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("Enchantments");
            if (enchantSection != null)
                for (String enchant : enchantSection.getKeys(false))
                    item.addEnchant(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)),
                            enchantSection.getInt(enchant));
        }
    }

    private void parseOraxenSections(ItemBuilder item) {
        ConfigurationSection merged = mergeWithTemplateSection();
        ConfigurationSection mechanicsSection = merged.getConfigurationSection("Mechanics");
        if (mechanicsSection != null)
            for (String mechanicID : mechanicsSection.getKeys(false)) {
                MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);

                if (factory != null) {
                    ConfigurationSection mechanicSection = mechanicsSection.getConfigurationSection(mechanicID);
                    if (mechanicSection == null)
                        continue;
                    Mechanic mechanic = factory.parse(mechanicSection);
                    if (mechanic == null)
                        continue;
                    // Apply item modifiers
                    for (Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                        item = itemModifier.apply(item);
                }
            }

        //// starting from 1.21.4, we no longer use Custom Model Data to set the item
        //// appearance
        //if (oraxenMeta.hasPackInfos() && VersionUtil.atOrAbove("1.21.4")) {
        //    // if there is not an item model component overriding it, we set its value
        //    // to the automatically created item model definition
        //    if (!item.hasItemModel()) {
        //        item.setItemModel(new NamespacedKey(OraxenPlugin.get(), section.getName()));
        //    }
        //} else {
        //    Integer customModelData;
        //    if (MODEL_DATAS_BY_ID.containsKey(section.getName())) {
        //        customModelData = MODEL_DATAS_BY_ID.get(section.getName()).getModelData();
        //    } else if (!item.hasItemModel()) {
        //        customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
        //        configUpdated = true;
        //        if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool())
        //            Optional.ofNullable(section.getConfigurationSection("Pack"))
        //                    .ifPresent(c -> c.set("custom_model_data", customModelData));
        //    } else
        //        customModelData = null;
        //    if (customModelData != null) {
        //        item.setCustomModelData(customModelData);
        //        oraxenMeta.setCustomModelData(customModelData);
        //    }
        //}
        ModelData modelData = MODEL_DATAS_BY_ID.get(section.getName());
        if (modelData == null) {
            return;
        }
        item.setCustomModelData(modelData.getModelData());
    }

    private ConfigurationSection mergeWithTemplateSection() {
        if (section == null || templateItem == null || templateItem.section == null)
            return section;

        ConfigurationSection merged = new YamlConfiguration().createSection(section.getName());
        OraxenYaml.copyConfigurationSection(templateItem.section, merged);
        OraxenYaml.copyConfigurationSection(section, merged);
        merged.set("injectId", true);

        return merged;
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }

}
