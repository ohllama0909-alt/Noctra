package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;

public abstract class ModuleSetting<T> {

    private final String id;
    private final String label;
    private final T defaultValue;
    private T value;

    protected ModuleSetting(String id, String label, T defaultValue) {
        this.id           = id;
        this.label        = label;
        this.defaultValue = defaultValue;
        this.value        = defaultValue;
    }

    public String getId()      { return id; }
    public String getLabel()   { return label; }
    public T      getDefault() { return defaultValue; }
    public T      get()        { return value; }

    public void set(T raw)  { this.value = validate(raw); }
    public void reset()     { this.value = defaultValue; }

    protected abstract T validate(T raw);

    public abstract T          fromJson(JsonElement el);
    public abstract JsonElement toJson();
}
