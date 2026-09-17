package de.snenjih.noctra.modules.api;

import de.snenjih.noctra.config.ModConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton registry for HUD elements. Tracks all registered HUD overlays
 * and their positions/sizes (persisted in ModConfig).
 */
public final class HudRegistry {

    public record HudEntry(HudElement element, int defaultX, int defaultY) {}

    private static final List<HudEntry> ENTRIES = new ArrayList<>();

    private HudRegistry() {}

    /**
     * Register a HUD element with its default top-left position.
     * Call from NoctraMod.onInitializeClient() after the owning module is registered.
     */
    public static void register(HudElement element, int defaultX, int defaultY) {
        ENTRIES.add(new HudEntry(element, defaultX, defaultY));
        // Ensure a state exists in config (initializes from defaults if missing)
        ModConfig cfg = ModConfig.getInstance();
        if (cfg != null) {
            cfg.initHudState(element.getHudId(), defaultX, defaultY,
                    element.getDefaultWidth(), element.getDefaultHeight());
        }
    }

    /** Returns an unmodifiable view of all registered HUD entries. */
    public static List<HudEntry> getAll() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /** Find an entry by HUD element ID. Returns null if not found. */
    public static HudEntry getById(String hudId) {
        for (HudEntry e : ENTRIES) {
            if (e.element().getHudId().equals(hudId)) return e;
        }
        return null;
    }

    /** Clear all registrations (for testing/re-init). */
    public static void clear() {
        ENTRIES.clear();
    }
}
