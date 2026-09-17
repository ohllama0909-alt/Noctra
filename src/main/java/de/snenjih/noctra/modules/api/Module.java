package de.snenjih.noctra.modules.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public interface Module {

    String         getId();
    String         getName();
    String         getDescription();
    Identifier     getIconTexture();
    ModuleCategory getCategory();
    boolean        isEnabled();
    void           setEnabled(boolean enabled);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    default void onEnable()  {}
    default void onDisable() {}

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    default void onClientTick(MinecraftClient client) {}

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    /** Right-click item interaction. Return PASS to let vanilla handle it. */
    default ActionResult onInteractItem(ClientPlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    /** Left-click attack on an entity. Return PASS to let vanilla handle it. */
    default ActionResult onAttackEntity(ClientPlayerEntity player, Entity target) {
        return ActionResult.PASS;
    }

    // -------------------------------------------------------------------------
    // Chat
    // -------------------------------------------------------------------------

    /**
     * Outgoing chat message. Return PASS to allow sending, SUCCESS/FAIL to cancel.
     * Only called when the module is enabled.
     */
    default ActionResult onSendChat(String message) {
        return ActionResult.PASS;
    }

    /**
     * Incoming chat/game message. Return PASS to allow the message, FAIL to suppress it.
     * Only called when the module is enabled.
     */
    default ActionResult onReceiveChat(Text message) { return ActionResult.PASS; }

    // -------------------------------------------------------------------------
    // World events
    // -------------------------------------------------------------------------

    default void onJoinWorld(ClientWorld world) {}
    default void onLeaveWorld()                 {}

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    default void onRenderHud(DrawContext ctx, float tickDelta) {}
    default void onRenderWorld(WorldRenderContext ctx)         {}
}
