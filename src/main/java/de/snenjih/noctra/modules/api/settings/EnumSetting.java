package de.snenjih.noctra.modules.api.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class EnumSetting<E extends Enum<E>> extends ModuleSetting<E> {

    private final Class<E> type;

    public EnumSetting(String id, String label, E defaultValue, Class<E> type) {
        super(id, label, defaultValue);
        this.type = type;
    }

    public E[] values() { return type.getEnumConstants(); }

    @Override protected E          validate(E raw)          { return raw != null ? raw : getDefault(); }
    @Override public    E          fromJson(JsonElement el)  { return Enum.valueOf(type, el.getAsString()); }
    @Override public    JsonElement toJson()                 { return new JsonPrimitive(get().name()); }
}
