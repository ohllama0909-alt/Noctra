package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends ModuleSetting<Boolean> {

    public BooleanSetting(String id, String label, boolean defaultValue) {
        super(id, label, defaultValue);
    }

    @Override protected Boolean    validate(Boolean raw) { return raw; }
    @Override public    Boolean    fromJson(JsonElement el) { return el.getAsBoolean(); }
    @Override public    JsonElement toJson()               { return new JsonPrimitive(get()); }
}
