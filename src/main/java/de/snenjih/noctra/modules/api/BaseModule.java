package de.snenjih.noctra.modules.api;

import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseModule implements Module {

    /** An entry in the ordered settings list — either a setting or a section header. */
    public sealed interface SettingEntry permits SettingEntry.Setting, SettingEntry.SectionHeader {
        record Setting(ModuleSetting<?> setting) implements SettingEntry {}
        record SectionHeader(String label) implements SettingEntry {}
    }

    private final String         id;
    private final String         name;
    private final String         description;
    private final ModuleCategory category;
    private final Identifier     icon;
    private       boolean        enabled  = false;

    private final List<ModuleSetting<?>> settings      = new ArrayList<>();
    private final List<SettingEntry>     settingEntries = new ArrayList<>();

    protected BaseModule(String id, String name, String description,
                         ModuleCategory category, Identifier icon) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.category    = category;
        this.icon        = icon;
    }

    @Override public final String         getId()          { return id; }
    @Override public final String         getName()        { return name; }
    @Override public final String         getDescription() { return description; }
    @Override public final ModuleCategory getCategory()    { return category; }
    @Override public final Identifier     getIconTexture() { return icon; }
    @Override public final boolean        isEnabled()      { return enabled; }

    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    /**
     * Add a new collapsible section header. All settings registered after this call
     * (until the next beginSection or end of constructor) appear under this section
     * in the settings screen.
     */
    protected void beginSection(String label) {
        settingEntries.add(new SettingEntry.SectionHeader(label));
    }

    protected <T> ModuleSetting<T> addSetting(ModuleSetting<T> setting) {
        settings.add(setting);
        settingEntries.add(new SettingEntry.Setting(setting));
        return setting;
    }

    /** Flat list of all settings (no section markers). */
    public List<ModuleSetting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /** Ordered list including section headers, for use by the settings screen. */
    public List<SettingEntry> getSettingEntries() {
        return Collections.unmodifiableList(settingEntries);
    }

    /**
     * Override to render a custom preview panel in the module's settings screen.
     * The panel area is at the bottom of the settings screen, fixed height 80px.
     * Default implementation is a no-op (no preview shown).
     */
    public void renderSettingsPreview(DrawContext ctx, int x, int y, int w, int h) {}

    /**
     * Override to provide custom widgets appended after the standard settings.
     * Default returns an empty list.
     */
    public List<CustomWidget> getCustomWidgets() {
        return Collections.emptyList();
    }
}
