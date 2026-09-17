package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class IntSetting extends ModuleSetting<Integer> {

    private final int min;
    private final int max;

    public IntSetting(String id, String label, int defaultValue, int min, int max) {
        super(id, label, defaultValue);
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override protected Integer    validate(Integer raw)   { return Math.clamp(raw, min, max); }
    @Override public    Integer    fromJson(JsonElement el) { return el.getAsInt(); }
    @Override public    JsonElement toJson()               { return new JsonPrimitive(get()); }
}
