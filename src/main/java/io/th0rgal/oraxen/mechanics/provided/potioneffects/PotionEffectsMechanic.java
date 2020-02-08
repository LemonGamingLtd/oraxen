package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PotionEffectsMechanic extends Mechanic {

    private Map<Position, Set<PotionEffect>> effects = new HashMap<>();
    private Map<Position, Set<PotionEffectType>> overridedTypes = new HashMap<>();
    private Map<UUID, Set<PotionEffect>> previousPlayerEffects = new HashMap<>();

    enum Position {
        HELD,
        WORN
    }

    public PotionEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        for (String effectSection : section.getKeys(false))
            if (section.isConfigurationSection(effectSection))
                registersEffectFromSection(section.getConfigurationSection(effectSection));
    }

    public void registersEffectFromSection(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        int amplifier = 0;
        boolean ambient = false;
        boolean particles = true;
        boolean icon = true;
        if (section.isInt("amplifier"))
            amplifier = section.getInt("amplifier");
        if (section.isBoolean("ambient"))
            ambient = section.getBoolean("ambient");
        if (section.isBoolean("particles"))
            particles = section.getBoolean("particles");
        if (section.isBoolean("icon"))
            icon = section.getBoolean("icon");
        PotionEffect potionEffect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, ambient, particles, icon);
        Position position = Position.valueOf(section.getString("position").toUpperCase());

        Set<PotionEffect> effectsPerPosition = effects.getOrDefault(position, new HashSet<>());
        effectsPerPosition.add(potionEffect);
        effects.put(position, effectsPerPosition);

        Set<PotionEffectType> overridedTypesPerPosition = overridedTypes.getOrDefault(position, new HashSet<>());
        overridedTypesPerPosition.add(potionEffect.getType());
        overridedTypes.put(position, overridedTypesPerPosition);
    }

    public void onItemPlaced(Position position, Player player) {
        if (!effects.containsKey(position))
            return;
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        Set<PotionEffect> currentConflictingEffects = new HashSet<>();
        for (PotionEffect potionEffect : activeEffects)
            // to avoid player lose their previous effects or can usebug
            if (potionEffect.getDuration() < 1728000 && !hasConflict(position, potionEffect))
                currentConflictingEffects.add(potionEffect);

        if (!currentConflictingEffects.isEmpty())
            previousPlayerEffects.put(player.getUniqueId(), currentConflictingEffects);

        player.addPotionEffects(effects.get(position));
    }

    public void onItemRemoved(Position position, Player player) {
        if (!overridedTypes.containsKey(position))
            return;
        for (PotionEffectType potionEffectType : overridedTypes.get(position))
            player.removePotionEffect(potionEffectType);

        if (previousPlayerEffects.containsKey(player.getUniqueId())) {
            player.addPotionEffects(previousPlayerEffects.get(player.getUniqueId()));
            previousPlayerEffects.remove(player.getUniqueId());
        }
    }

    private boolean hasConflict(Position position, PotionEffect potionEffect) {
        return overridedTypes.get(position).contains(potionEffect.getType());
    }

}