package dev.aero.cnmterraincompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;

/** Applies provider-declared render metadata to CNM-owned generated models. */
public final class ProviderVisualAdapter {
    private ProviderVisualAdapter() {}

    public static JsonElement decorateModel(NibaruMaterialProfile profile, JsonElement model) {
        if (!(model instanceof JsonObject object)) return model;
        String renderType = switch (profile.renderLayer()) {
            case SOLID -> null;
            case CUTOUT -> "cutout";
            case CUTOUT_MIPPED -> "cutout_mipped";
            case TRANSLUCENT -> "translucent";
        };
        if (renderType != null) object.addProperty("render_type", renderType);
        return object;
    }
}
