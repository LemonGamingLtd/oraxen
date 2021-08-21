package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public enum Settings {

    PLUGIN_LANGUAGE("Plugin.language"),
    REPAIR_COMMAND_ORAXEN_DURABILITY("Plugin.commands.repair.oraxen_durability_only"),

    CONFIGS_VERSION("configs_version"),
    UPDATE_CONFIGS("ConfigsTools.enable_configs_updater"),
    AUTOMATICALLY_SET_GLYPH_CODE("ConfigsTools.automatically_set_glyph_code"),
    AUTOMATICALLY_SET_MODEL_DATA("ConfigsTools.automatically_set_model_data"),
    ERROR_ITEM("ConfigsTools.error_item"),

    RESET_RECIPES("Misc.reset_recipes"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display"),
    CROSSBOW_DISPLAY("Misc.crossbow_display"),
    AUTO_UPDATE_ITEMS("Misc.auto_update_items"),

    GENERATE("Pack.generation.generate"),
    COMPRESSION("Pack.generation.compression"),
    PROTECTION("Pack.generation.protection"),
    COMMENT("Pack.generation.comment"),

    UPLOAD_TYPE("Pack.upload.type"),
    UPLOAD("Pack.upload.enabled"),
    UPLOAD_OPTIONS("Pack.upload.options"),

    POLYMATH_SERVER("Pack.upload.polymath.server"),

    SEND_PACK("Pack.dispatch.send_pack"),
    SEND_PACK_DELAY("Pack.dispatch.delay"),
    SEND_PACK_ADVANCED("Pack.dispatch.send_pack_advanced.enabled"),
    SEND_PACK_ADVANCED_MANDATORY("Pack.dispatch.send_pack_advanced.mandatory"),
    SEND_PACK_ADVANCED_MESSAGE("Pack.dispatch.send_pack_advanced.message"),
    SEND_JOIN_MESSAGE("Pack.dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("Pack.dispatch.join_message.delay"),

    RECEIVE_ENABLED("Pack.receive.enabled"),

    RECEIVE_ALLOWED_SEND_MESSAGE("Pack.receive.accepted.actions.message.enabled"),
    RECEIVE_ALLOWED_MESSAGE_PERIOD("Pack.receive.accepted.actions.message.period"),
    RECEIVE_ALLOWED_MESSAGE_DELAY("Pack.receive.accepted.actions.message.delay"),
    RECEIVE_ALLOWED_MESSAGE_ACTION("Pack.receive.accepted.actions.message.type"),
    RECEIVE_ALLOWED_MESSAGE("Pack.receive.accepted.actions.message.content"),
    RECEIVE_ALLOWED_COMMANDS("Pack.receive.accepted.actions.commands"),

    RECEIVE_LOADED_SEND_MESSAGE("Pack.receive.loaded.actions.message.enabled"),
    RECEIVE_LOADED_MESSAGE_PERIOD("Pack.receive.loaded.actions.message.period"),
    RECEIVE_LOADED_MESSAGE_DELAY("Pack.receive.loaded.actions.message.delay"),
    RECEIVE_LOADED_MESSAGE_ACTION("Pack.receive.loaded.actions.message.type"),
    RECEIVE_LOADED_MESSAGE("Pack.receive.loaded.actions.message.content"),
    RECEIVE_LOADED_COMMANDS("Pack.receive.loaded.actions.commands"),

    RECEIVE_FAILED_SEND_MESSAGE("Pack.receive.failed_download.actions.message.enabled"),
    RECEIVE_FAILED_MESSAGE_PERIOD("Pack.receive.failed_download.actions.message.period"),
    RECEIVE_FAILED_MESSAGE_DELAY("Pack.receive.failed_download.actions.message.delay"),
    RECEIVE_FAILED_MESSAGE_ACTION("Pack.receive.failed_download.actions.message.type"),
    RECEIVE_FAILED_MESSAGE("Pack.receive.failed_download.actions.message.content"),
    RECEIVE_FAILED_COMMANDS("Pack.receive.failed_download.actions.commands"),

    RECEIVE_DENIED_SEND_MESSAGE("Pack.receive.denied.actions.message.enabled"),
    RECEIVE_DENIED_MESSAGE_PERIOD("Pack.receive.denied.actions.message.period"),
    RECEIVE_DENIED_MESSAGE_DELAY("Pack.receive.denied.actions.message.delay"),
    RECEIVE_DENIED_MESSAGE_ACTION("Pack.receive.denied.actions.message.type"),
    RECEIVE_DENIED_MESSAGE("Pack.receive.denied.actions.message.content"),
    RECEIVE_DENIED_COMMANDS("Pack.receive.denied.actions.commands");

    private final String path;

    Settings(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return OraxenPlugin.get().getConfigsManager().getSettings().get(path);
    }

    @Override
    public String toString() {
        return (String) getValue();
    }

    public Boolean toBool() {
        return (Boolean) getValue();
    }

    public List<String> toStringList() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getStringList(path);
    }

    public ConfigurationSection toConfigSection() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getConfigurationSection(path);
    }

}
