package io.th0rgal.oraxen.hook;

import ltd.lemongaming.packgenerator.PackManager;
import ltd.lemongaming.packgenerator.annotation.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class PackGeneratorPluginHook {

    /**
     * List of all custom models.
     */
    private static final Set<Resource> CUSTOM_MODELS = PackManager.getAllResources().stream().collect(Collectors.toUnmodifiableSet());

    @NotNull
    public static Optional<Resource> getCustomModel(String name) {
        return CUSTOM_MODELS.stream().filter(model -> model.name().equalsIgnoreCase(name)).findFirst();
    }

}
