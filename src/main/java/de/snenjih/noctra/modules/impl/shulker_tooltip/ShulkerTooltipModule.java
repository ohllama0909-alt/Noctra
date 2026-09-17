package de.snenjih.noctra.modules.impl.shulker_tooltip;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.KeybindSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class ShulkerTooltipModule extends BaseModule {

    public static ShulkerTooltipModule INSTANCE;

    public final ModuleSetting<Boolean> alwaysOn;
    public final ModuleSetting<Integer> previewKey;
    public final ModuleSetting<Integer> fullPreviewKey;
    public final ModuleSetting<Boolean> swapModes;
    public final ModuleSetting<Boolean> compactMode;
    public final ModuleSetting<Boolean> compactSortByCount;
    public final ModuleSetting<Integer> maxRowSize;
    public final ModuleSetting<Boolean> shortCounts;
    public final ModuleSetting<Boolean> genericContainerPreview;
    public final ModuleSetting<Boolean> showBundles;
    public final ModuleSetting<Boolean> coloredPreview;
    public final ModuleSetting<Boolean> showKeyHint;

    public ShulkerTooltipModule() {
        super(
            "shulker_tooltip",
            "Shulker Tooltip",
            "Shows shulker box and bundle contents as a preview grid. Hold Shift to show.",
            ModuleCategory.UTILITY,
            Identifier.of("noctra", "modules/shulker_tooltip")
        );
        INSTANCE = this;

        beginSection("Preview");
        alwaysOn              = addSetting(new BooleanSetting("always_on",                 "Always Show Preview",       false));
        previewKey            = addSetting(new KeybindSetting("preview_key",               "Preview Key",               GLFW.GLFW_KEY_LEFT_SHIFT));
        fullPreviewKey        = addSetting(new KeybindSetting("full_preview_key",          "Full Preview Key",          GLFW.GLFW_KEY_LEFT_ALT));
        swapModes             = addSetting(new BooleanSetting("swap_modes",                "Swap Preview Modes",        false));
        compactMode           = addSetting(new BooleanSetting("compact_mode",              "Compact Mode",              false));
        compactSortByCount    = addSetting(new BooleanSetting("compact_sort_by_count",     "Sort Compact by Count",     true));
        maxRowSize            = addSetting(new IntSetting    ("max_row_size",              "Max Items Per Row",         9, 1, 18));
        shortCounts           = addSetting(new BooleanSetting("short_counts",              "Short Item Counts",         true));
        genericContainerPreview = addSetting(new BooleanSetting("generic_container_preview", "Generic Container Preview", true));

        beginSection("Display");
        coloredPreview        = addSetting(new BooleanSetting("colored_preview",           "Colored Background",        true));
        showKeyHint           = addSetting(new BooleanSetting("show_key_hint",             "Show Key Hint",             true));
        showBundles           = addSetting(new BooleanSetting("show_bundles",              "Preview Bundles",           true));
    }

    /** Returns true if the preview key is held or alwaysOn is set. */
    public boolean isPreviewActive() {
        if (alwaysOn.get()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        return InputUtil.isKeyPressed(mc.getWindow(), previewKey.get())
            || InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** Returns true if the full-preview override key is held. */
    public boolean isFullPreviewKeyHeld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        return InputUtil.isKeyPressed(mc.getWindow(), fullPreviewKey.get());
    }

    /**
     * Determines whether compact mode should be active.
     * Normal: previewKey shows compact (if compactMode=true), fullPreviewKey overrides to full.
     * Swapped: previewKey always shows full, fullPreviewKey overrides to compact.
     */
    public boolean shouldUseCompactMode() {
        boolean base = compactMode.get();
        boolean fullKeyHeld = isFullPreviewKeyHeld();
        if (swapModes.get()) {
            // swapped: previewKey=full, fullPreviewKey=compact
            return fullKeyHeld || base;
        } else {
            // normal: previewKey=compact (respects compactMode), fullPreviewKey=full override
            return base && !fullKeyHeld;
        }
    }

    /**
     * Returns the list of items for this container stack, or null if not a supported container.
     * Full mode: 27-slot list preserving slot positions (may contain empty stacks).
     * Compact mode: merged list of non-empty items only.
     */
    public List<ItemStack> getContainerItems(ItemStack stack) {
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            if (!genericContainerPreview.get()
                    && !(stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock)) {
                return null;
            }
            boolean useCompact = shouldUseCompactMode();
            if (useCompact) {
                return mergeItems(container.streamNonEmpty()
                    .map(ItemStack::copy)
                    .collect(Collectors.toList()));
            }
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.copyTo(stacks);
            return stacks;
        }
        // Bundle
        if (showBundles.get()) {
            BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundle != null) {
                List<ItemStack> bundleItems = bundle.stream()
                    .map(ItemStack::copy)
                    .collect(Collectors.toList());
                if (shouldUseCompactMode()) {
                    return mergeItems(bundleItems);
                }
                return bundleItems;
            }
        }
        return null;
    }

    /** Total number of slots for the full-mode grid layout. */
    public int getTotalSlots(ItemStack stack) {
        if (stack.get(DataComponentTypes.CONTAINER) != null) {
            return 27;
        }
        BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundle != null) {
            return (int) bundle.stream().count();
        }
        return 27;
    }

    /** Returns the background color for the preview window. */
    public int getBackgroundColor(ItemStack stack) {
        if (coloredPreview.get() && stack.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof ShulkerBoxBlock shulker) {
            DyeColor color = shulker.getColor();
            if (color != null) {
                int rgb = color.getEntityColor();
                int r = Math.max(30, (rgb >> 16 & 0xFF) / 3);
                int g = Math.max(30, (rgb >> 8  & 0xFF) / 3);
                int b = Math.max(30, (rgb        & 0xFF) / 3);
                return 0xAA000000 | (r << 16) | (g << 8) | b;
            }
        }
        return 0xAA0D1B2A;
    }

    /** Merge items of same type, summing counts. Optionally sorted by count descending. */
    private List<ItemStack> mergeItems(List<ItemStack> input) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack s : input) {
            if (!s.isEmpty()) counts.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : counts.entrySet()) {
            result.add(new ItemStack(e.getKey(), e.getValue()));
        }
        if (compactSortByCount.get()) {
            result.sort(Comparator.comparingInt((ItemStack s) -> -s.getCount()));
        }
        return result;
    }
}
