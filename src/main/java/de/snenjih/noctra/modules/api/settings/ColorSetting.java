package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Stores an ARGB color as an int (0xAARRGGBB). */
public final class ColorSetting extends ModuleSetting<Integer> {

    public ColorSetting(String id, String label, int defaultArgb) {
        super(id, label, defaultArgb);
    }

    @Override protected Integer    validate(Integer raw) { return raw != null ? raw : getDefault(); }
    @Override public    Integer    fromJson(JsonElement el) { return el.getAsInt(); }
    @Override public    JsonElement toJson()               { return new JsonPrimitive(get()); }
}
