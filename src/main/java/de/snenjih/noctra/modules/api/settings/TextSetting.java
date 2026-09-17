package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Stores a String value with optional max length. */
public final class TextSetting extends ModuleSetting<String> {

    private final int maxLength;

    public TextSetting(String id, String label, String defaultValue, int maxLength) {
        super(id, label, defaultValue);
        this.maxLength = maxLength;
    }

    public int getMaxLength() { return maxLength; }

    @Override
    protected String validate(String raw) {
        if (raw == null) return getDefault();
        return raw.length() <= maxLength ? raw : raw.substring(0, maxLength);
    }

    @Override public String      fromJson(JsonElement el) { return el.getAsString(); }
    @Override public JsonElement toJson()                 { return new JsonPrimitive(get()); }
}
