package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class FloatSetting extends ModuleSetting<Float> {

    private final float min;
    private final float max;

    public FloatSetting(String id, String label, float defaultValue, float min, float max) {
        super(id, label, defaultValue);
        this.min = min;
        this.max = max;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }

    @Override
    protected Float validate(Float raw) {
        if (Float.isNaN(raw)) return getDefault();
        return Math.clamp(raw, min, max);
    }

    @Override public Float      fromJson(JsonElement el) { return el.getAsFloat(); }
    @Override public JsonElement toJson()                { return new JsonPrimitive(get()); }
}
