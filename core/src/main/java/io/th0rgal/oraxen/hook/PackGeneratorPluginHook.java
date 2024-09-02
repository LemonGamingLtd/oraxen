package io.th0rgal.oraxen.hook;

import ltd.lemongaming.packgenerator.PackManager;
import ltd.lemongaming.packgenerator.annotation.CustomModel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class PackGeneratorPluginHook {

    /**
     * List of all custom models.
     */
    private static final Set<CustomModel> CUSTOM_MODELS = PackManager.getAllResources().stream()
        .filter(resource -> resource instanceof CustomModel)
        .map(resource -> (CustomModel) resource)
        .collect(Collectors.toUnmodifiableSet());

    @NotNull
    public static Optional<CustomModel> getCustomModel(String name) {
        return CUSTOM_MODELS.stream().filter(model -> model.name().equalsIgnoreCase(name)).findFirst();
    }

}
