package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Stores a GLFW key code. -1 = unbound. */
public final class KeybindSetting extends ModuleSetting<Integer> {

    public KeybindSetting(String id, String label, int defaultGlfwKey) {
        super(id, label, defaultGlfwKey);
    }

    @Override protected Integer    validate(Integer raw) { return raw != null ? raw : -1; }
    @Override public    Integer    fromJson(JsonElement el) { return el.getAsInt(); }
    @Override public    JsonElement toJson()               { return new JsonPrimitive(get()); }
}
