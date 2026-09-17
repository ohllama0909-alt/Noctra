package de.snenjih.noctra;

import de.snenjih.noctra.auth.AccountManager;
import de.snenjih.noctra.auth.AccountStorage;
import de.snenjih.noctra.auth.SkinCache;
import de.snenjih.noctra.chat.ChatCommandDispatcher;
import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.hud.NotificationManager;
import de.snenjih.noctra.input.KeybindManager;
import de.snenjih.noctra.menu.MainMenuScreen;
import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.HudElement;
import de.snenjih.noctra.modules.api.HudRegistry;
import de.snenjih.noctra.modules.api.Module;
import de.snenjih.noctra.modules.impl.anti_afk.AntiAfkModule;
import de.snenjih.noctra.modules.impl.armor_status_hud.ArmorStatusHudModule;
import de.snenjih.noctra.modules.impl.auto_eat.AutoEatModule;
import de.snenjih.noctra.modules.impl.auto_totem.AutoTotemModule;
import de.snenjih.noctra.modules.impl.coordinates_hud.CoordinatesHudModule;
import de.snenjih.noctra.modules.impl.death_coordinates.DeathCoordinatesModule;
import de.snenjih.noctra.modules.impl.elytra_swap.ElytraSwapModule;
import de.snenjih.noctra.modules.impl.food_tooltip.FoodTooltipModule;
import de.snenjih.noctra.modules.impl.fps_ping_hud.FpsPingHudModule;
import de.snenjih.noctra.modules.impl.inventory_lock.InventoryLockModule;
import de.snenjih.noctra.modules.impl.keystrokes_hud.KeystrokesHudModule;
import de.snenjih.noctra.modules.impl.middle_click_pick.MiddleClickPickModule;
import de.snenjih.noctra.modules.impl.potion_effects_hud.PotionEffectsHudModule;
import de.snenjih.noctra.modules.impl.smart_replace.SmartReplaceModule;
import de.snenjih.noctra.modules.impl.biome_display.BiomeDisplayModule;
import de.snenjih.noctra.modules.impl.crosshair_customizer.CrosshairCustomizerModule;
import de.snenjih.noctra.modules.impl.durability_hud.DurabilityHudModule;
import de.snenjih.noctra.modules.impl.fullbright.FullbrightModule;
import de.snenjih.noctra.modules.impl.hit_color.HitColorModule;
import de.snenjih.noctra.modules.impl.item_age_timer.ItemAgeTimerModule;
import de.snenjih.noctra.modules.impl.mc_time_display.McTimeDisplayModule;
import de.snenjih.noctra.modules.impl.rain_disable.RainDisableModule;
import de.snenjih.noctra.modules.impl.real_time_clock.RealTimeClockModule;
import de.snenjih.noctra.modules.impl.saturation_bar.SaturationBarModule;
import de.snenjih.noctra.modules.impl.scoreboard_hud.ScoreboardHudModule;
import de.snenjih.noctra.modules.impl.speed_display.SpeedDisplayModule;
import de.snenjih.noctra.modules.impl.zoom.ZoomModule;
import de.snenjih.noctra.modules.impl.sneak_toggle.SneakToggleModule;
import de.snenjih.noctra.modules.impl.sprint_toggle.SprintToggleModule;
import de.snenjih.noctra.modules.impl.stack_refill.StackRefillModule;
import de.snenjih.noctra.modules.impl.nametag_badge.NametageModule;
import de.snenjih.noctra.modules.impl.target_hp.TargetHpModule;
import de.snenjih.noctra.modules.impl.mod_settings.ModSettingsModule;
import de.snenjih.noctra.modules.impl.tool_selector.ToolSelectorModule;
import de.snenjih.noctra.modules.impl.anti_fog.AntiFogModule;
import de.snenjih.noctra.modules.impl.anti_vignette.AntiVignetteModule;
import de.snenjih.noctra.modules.impl.item_info_hud.ItemInfoHudModule;
import de.snenjih.noctra.modules.impl.boss_bar_customizer.BossBarCustomizerModule;
import de.snenjih.noctra.modules.impl.damage_indicator.DamageIndicatorModule;
import de.snenjih.noctra.modules.impl.held_item_info.HeldItemInfoModule;
import de.snenjih.noctra.modules.impl.tps_display.TpsDisplayModule;
import de.snenjih.noctra.modules.impl.memory_usage_hud.MemoryUsageHudModule;
import de.snenjih.noctra.modules.impl.chunk_render_hud.ChunkRenderHudModule;
import de.snenjih.noctra.modules.impl.day_counter.DayCounterModule;
import de.snenjih.noctra.modules.impl.xp_level_hud.XpLevelHudModule;
import de.snenjih.noctra.modules.impl.item_counter.ItemCounterModule;
import de.snenjih.noctra.modules.impl.stack_counter.StackCounterModule;
import de.snenjih.noctra.modules.impl.altitude_hud.AltitudeHudModule;
import de.snenjih.noctra.modules.impl.redstone_signal_hud.RedstoneSignalHudModule;
import de.snenjih.noctra.modules.impl.speedometer.SpeedometerModule;
import de.snenjih.noctra.modules.impl.server_address_hud.ServerAddressHudModule;
import de.snenjih.noctra.modules.impl.attack_cooldown_indicator.AttackCooldownIndicatorModule;
import de.snenjih.noctra.modules.impl.hit_indicator.HitIndicatorModule;
import de.snenjih.noctra.modules.impl.kill_counter.KillCounterModule;
import de.snenjih.noctra.modules.impl.cps_counter.CpsCounterModule;
import de.snenjih.noctra.modules.impl.auto_shield.AutoShieldModule;
import de.snenjih.noctra.modules.impl.reach_display.ReachDisplayModule;
import de.snenjih.noctra.modules.impl.arrow_counter.ArrowCounterModule;
import de.snenjih.noctra.modules.impl.damage_dealt_hud.DamageDealtHudModule;
import de.snenjih.noctra.modules.impl.combo_counter.ComboCounterModule;
import de.snenjih.noctra.modules.impl.pitch_lock.PitchLockModule;
import de.snenjih.noctra.modules.impl.glide_stats.GlideStatsModule;
import de.snenjih.noctra.modules.impl.firework_boost.FireworkBoostModule;
import de.snenjih.noctra.modules.impl.elytra_landing_swap.ElytraLandingSwapModule;
import de.snenjih.noctra.modules.impl.mention_highlight.MentionHighlightModule;
import de.snenjih.noctra.modules.impl.message_filter.MessageFilterModule;
import de.snenjih.noctra.modules.impl.quick_messages.QuickMessagesModule;
import de.snenjih.noctra.modules.impl.copy_coords.CopyCoordsModule;
import de.snenjih.noctra.modules.impl.waypoints.WaypointsModule;
import de.snenjih.noctra.modules.impl.light_level_overlay.LightLevelOverlayModule;
import de.snenjih.noctra.modules.impl.chest_highlight.ChestHighlightModule;
import de.snenjih.noctra.modules.impl.cave_finder.CaveFinderModule;
import de.snenjih.noctra.modules.impl.slime_chunks.SlimeChunksModule;
import de.snenjih.noctra.modules.impl.shulker_tooltip.ShulkerTooltipComponent;
import de.snenjih.noctra.modules.impl.shulker_tooltip.ShulkerTooltipData;
import de.snenjih.noctra.modules.impl.shulker_tooltip.ShulkerTooltipModule;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import de.snenjih.noctra.cosmetics.network.CosmeticNetworkHandler;
import de.snenjih.noctra.cosmetics.render.CosmeticFeatureRenderer;
import de.snenjih.noctra.cosmetics.render.ParticleEmitter;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import de.snenjih.noctra.cosmetics.storage.CosmeticStorage;
import de.snenjih.noctra.cosmetics.sync.CosmeticSyncService;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Environment(EnvType.CLIENT)
public class NoctraMod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Noctra");

    @Override
    public void onInitializeClient() {
        ModConfig config = new ModConfig();
        config.load();

        // Account Switcher — must be initialized before any screen renders
        AccountStorage accountStorage = AccountStorage.load();
        SkinCache skinCache = new SkinCache();
        AccountManager.init(accountStorage, skinCache);
        // Preload skins for all saved accounts
        accountStorage.getAll().forEach(e -> skinCache.ensureLoaded(e.uuid(), e.skinUrl()));

        ModuleRegistry registry = ModuleRegistry.create(config);
        registry.register(new ModSettingsModule());
        registry.register(new ElytraSwapModule());
        registry.register(new AutoTotemModule());
        registry.register(new StackRefillModule());
        registry.register(new AutoEatModule());
        registry.register(new ToolSelectorModule());
        registry.register(new SmartReplaceModule());
        registry.register(new AntiAfkModule());
        registry.register(new MiddleClickPickModule());

        // Death Coordinates — HUD element
        DeathCoordinatesModule dcm = new DeathCoordinatesModule();
        registry.register(dcm);
        HudRegistry.register(dcm, 4, 4);

        registry.register(new FoodTooltipModule());
        registry.register(new InventoryLockModule());
        registry.register(new SprintToggleModule());
        registry.register(new SneakToggleModule());
        registry.register(new FullbrightModule());
        registry.register(new NametageModule());

        // New HUD modules
        CoordinatesHudModule coordinatesHud = new CoordinatesHudModule();
        registry.register(coordinatesHud);
        HudRegistry.register(coordinatesHud, 4, 34);

        FpsPingHudModule fpsPingHud = new FpsPingHudModule();
        registry.register(fpsPingHud);
        HudRegistry.register(fpsPingHud, 4, 50);

        ArmorStatusHudModule armorHud = new ArmorStatusHudModule();
        registry.register(armorHud);
        HudRegistry.register(armorHud, 4, 74);

        PotionEffectsHudModule potionHud = new PotionEffectsHudModule();
        registry.register(potionHud);
        HudRegistry.register(potionHud, 4, 150);

        KeystrokesHudModule keystrokesHud = new KeystrokesHudModule();
        registry.register(keystrokesHud);
        HudRegistry.register(keystrokesHud, 300, 150);

        DurabilityHudModule durabilityHud = new DurabilityHudModule();
        registry.register(durabilityHud);
        HudRegistry.register(durabilityHud, 4, 200);

        TargetHpModule targetHp = new TargetHpModule();
        registry.register(targetHp);
        HudRegistry.register(targetHp, 4, 240);

        BiomeDisplayModule biomeDisplay = new BiomeDisplayModule();
        registry.register(biomeDisplay);
        HudRegistry.register(biomeDisplay, 4, 16);

        SpeedDisplayModule speedDisplay = new SpeedDisplayModule();
        registry.register(speedDisplay);
        HudRegistry.register(speedDisplay, 4, 270);

        RealTimeClockModule realTimeClock = new RealTimeClockModule();
        registry.register(realTimeClock);
        HudRegistry.register(realTimeClock, 4, 300);

        SaturationBarModule saturationBar = new SaturationBarModule();
        registry.register(saturationBar);
        HudRegistry.register(saturationBar, 4, 330);

        registry.register(new ZoomModule());
        registry.register(new CrosshairCustomizerModule());
        registry.register(new HitColorModule());
        registry.register(new ItemAgeTimerModule());
        registry.register(new RainDisableModule());

        ScoreboardHudModule scoreboardHud = new ScoreboardHudModule();
        registry.register(scoreboardHud);
        HudRegistry.register(scoreboardHud, 4, 360);

        McTimeDisplayModule mcTimeDisplay = new McTimeDisplayModule();
        registry.register(mcTimeDisplay);
        HudRegistry.register(mcTimeDisplay, 4, 530);

        registry.register(new AntiFogModule());
        registry.register(new AntiVignetteModule());

        ItemInfoHudModule itemInfoHud = new ItemInfoHudModule();
        registry.register(itemInfoHud);
        HudRegistry.register(itemInfoHud, 4, 570);

        registry.register(new BossBarCustomizerModule());

        registry.register(new DamageIndicatorModule());

        HeldItemInfoModule heldItemInfo = new HeldItemInfoModule();
        registry.register(heldItemInfo);
        HudRegistry.register(heldItemInfo, 4, 600);

        TpsDisplayModule tpsDisplay = new TpsDisplayModule();
        registry.register(tpsDisplay);
        HudRegistry.register(tpsDisplay, 4, 640);

        MemoryUsageHudModule memoryUsage = new MemoryUsageHudModule();
        registry.register(memoryUsage);
        HudRegistry.register(memoryUsage, 4, 680);

        ChunkRenderHudModule chunkRenderHud = new ChunkRenderHudModule();
        registry.register(chunkRenderHud);
        HudRegistry.register(chunkRenderHud, 4, 720);

        DayCounterModule dayCounter = new DayCounterModule();
        registry.register(dayCounter);
        HudRegistry.register(dayCounter, 4, 760);

        XpLevelHudModule xpLevel = new XpLevelHudModule();
        registry.register(xpLevel);
        HudRegistry.register(xpLevel, 4, 800);

        ItemCounterModule itemCounter = new ItemCounterModule();
        registry.register(itemCounter);
        HudRegistry.register(itemCounter, 4, 840);

        StackCounterModule stackCounter = new StackCounterModule();
        registry.register(stackCounter);
        HudRegistry.register(stackCounter, 4, 880);

        AltitudeHudModule altitudeHud = new AltitudeHudModule();
        registry.register(altitudeHud);
        HudRegistry.register(altitudeHud, 4, 920);

        RedstoneSignalHudModule redstoneSignalHud = new RedstoneSignalHudModule();
        registry.register(redstoneSignalHud);
        HudRegistry.register(redstoneSignalHud, 4, 960);

        SpeedometerModule speedometer = new SpeedometerModule();
        registry.register(speedometer);
        HudRegistry.register(speedometer, 4, 1000);

        ServerAddressHudModule serverAddressHud = new ServerAddressHudModule();
        registry.register(serverAddressHud);
        HudRegistry.register(serverAddressHud, 4, 1040);

        AttackCooldownIndicatorModule attackCooldown = new AttackCooldownIndicatorModule();
        registry.register(attackCooldown);
        HudRegistry.register(attackCooldown, 200, 200);

        HitIndicatorModule hitIndicator = new HitIndicatorModule();
        registry.register(hitIndicator);
        HudRegistry.register(hitIndicator, 4, 1080);

        KillCounterModule killCounter = new KillCounterModule();
        registry.register(killCounter);
        HudRegistry.register(killCounter, 4, 1100);

        CpsCounterModule cpsCounter = new CpsCounterModule();
        registry.register(cpsCounter);
        HudRegistry.register(cpsCounter, 4, 1120);

        registry.register(new AutoShieldModule());

        ReachDisplayModule reachDisplay = new ReachDisplayModule();
        registry.register(reachDisplay);
        HudRegistry.register(reachDisplay, 4, 1140);

        ArrowCounterModule arrowCounter = new ArrowCounterModule();
        registry.register(arrowCounter);
        HudRegistry.register(arrowCounter, 4, 1160);

        DamageDealtHudModule damageDealtHud = new DamageDealtHudModule();
        registry.register(damageDealtHud);
        HudRegistry.register(damageDealtHud, 4, 1180);

        ComboCounterModule comboCounter = new ComboCounterModule();
        registry.register(comboCounter);
        HudRegistry.register(comboCounter, 4, 1210);

        registry.register(new PitchLockModule());

        GlideStatsModule glideStats = new GlideStatsModule();
        registry.register(glideStats);
        HudRegistry.register(glideStats, 4, 1240);

        registry.register(new FireworkBoostModule());
        registry.register(new ElytraLandingSwapModule());
        registry.register(new MentionHighlightModule());
        registry.register(new MessageFilterModule());
        registry.register(new QuickMessagesModule());
        registry.register(new CopyCoordsModule());

        // World modules
        WaypointsModule waypoints = new WaypointsModule();
        registry.register(waypoints);
        HudRegistry.register(waypoints, 8, 60);

        registry.register(new LightLevelOverlayModule());
        registry.register(new ChestHighlightModule());
        registry.register(new CaveFinderModule());
        registry.register(new SlimeChunksModule());
        registry.register(new ShulkerTooltipModule());

        // Register ShulkerTooltip tooltip component factory
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ShulkerTooltipData std)
                return new ShulkerTooltipComponent(std);
            return null;
        });

        // Right-Shift opens the Noctra menu from in-game
        KeyBinding openMenuKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.noctra.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeybindManager.CATEGORY
            )
        );

        // ---- Cosmetics System ------------------------------------------------
        CosmeticStorage.init();
        CosmeticRegistry.init();
        CosmeticNetworkHandler.register();
        CosmeticSyncService.getInstance().syncAsync();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, renderer, registrationHelper, context) -> {
                if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                    //noinspection unchecked,rawtypes
                    ((LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper) registrationHelper)
                        .register(new CosmeticFeatureRenderer(playerRenderer));
                }
            }
        );

        registerEvents(registry, openMenuKey);
    }

    private static void registerEvents(ModuleRegistry registry, KeyBinding openMenuKey) {
        // ---- Tick -------------------------------------------------------
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeybindManager.onTick();
            NotificationManager.tick();
            // Right-Shift opens the carousel screen from in-game
            if (openMenuKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new MainMenuScreen(null));
            }
            for (Module m : registry.getAll()) {
                if (m.isEnabled()) {
                    try { m.onClientTick(client); }
                    catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onClientTick", m.getId(), e); }
                }
            }
        });

        // ---- HUD render -------------------------------------------------
        HudElementRegistry.addLast(
                Identifier.of("noctra", "hud"),
                (ctx, tickCounter) -> {
                    float delta = tickCounter.getTickProgress(false);
                    for (Module m : registry.getAll()) {
                        if (m.isEnabled()) {
                            try { m.onRenderHud(ctx, delta); }
                            catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onRenderHud", m.getId(), e); }
                        }
                    }
                    // Render all registered HUD elements
                    for (HudRegistry.HudEntry entry : HudRegistry.getAll()) {
                        HudElement elem = entry.element();
                        // Only render if it's a Module and that Module is enabled
                        if (elem instanceof Module m && !m.isEnabled()) continue;
                        ModConfig.HudElementState state = ModConfig.getInstance().getHudState(elem.getHudId());
                        if (state == null || !state.visible()) continue;
                        try {
                            elem.renderHud(ctx, delta, state.x(), state.y(), state.w(), state.h());
                        } catch (Exception e) {
                            LOGGER.error("[Noctra] {} crashed in HUD render", elem.getHudId(), e);
                        }
                    }
                    MinecraftClient mc = MinecraftClient.getInstance();
                    NotificationManager.render(ctx,
                            mc.getWindow().getScaledWidth(),
                            mc.getWindow().getScaledHeight());
                });

        // ---- World render -----------------------------------------------
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            for (Module m : registry.getAll()) {
                if (m.isEnabled()) {
                    try { m.onRenderWorld(ctx); }
                    catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onRenderWorld", m.getId(), e); }
                }
            }
            try { ParticleEmitter.onRenderWorld(ctx); }
            catch (Exception e) { LOGGER.error("[Noctra] Cosmetic ParticleEmitter crashed", e); }
        });

        // ---- World join / leave ----------------------------------------
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.world == null) return;
            for (Module m : registry.getAll()) {
                try { m.onJoinWorld(client.world); }
                catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onJoinWorld", m.getId(), e); }
            }
            // Send cosmetics state when joining a world
            CosmeticNetworkHandler.sendSelf();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            for (Module m : registry.getAll()) {
                try { m.onLeaveWorld(); }
                catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onLeaveWorld", m.getId(), e); }
            }
            // Clear other players' cosmetics on disconnect
            CosmeticRegistry.clearOtherPlayers();
        });

        // ---- Incoming chat (player chat messages) -----------------------
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            for (Module m : registry.getAll()) {
                if (!m.isEnabled()) continue;
                try {
                    ActionResult r = m.onReceiveChat(message);
                    if (r != ActionResult.PASS) return false;
                } catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onReceiveChat", m.getId(), e); }
            }
            return true;
        });

        // ---- Incoming game/system messages ------------------------------
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            for (Module m : registry.getAll()) {
                if (!m.isEnabled()) continue;
                try {
                    ActionResult r = m.onReceiveChat(message);
                    if (r != ActionResult.PASS) return false;
                } catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onReceiveChat", m.getId(), e); }
            }
            return true;
        });

        // ---- Outgoing chat (also handles client commands) ---------------
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            // Chat commands are consumed before server sees them
            if (ChatCommandDispatcher.handle(message)) return false;
            // Dispatch to modules that want to intercept chat
            for (Module m : registry.getAll()) {
                if (!m.isEnabled()) continue;
                try {
                    ActionResult result = m.onSendChat(message);
                    if (result != ActionResult.PASS) return false;
                } catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onSendChat", m.getId(), e); }
            }
            return true;
        });

        // ---- Attack entity ---------------------------------------------
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ClientPlayerEntity cp)) return ActionResult.PASS;
            for (Module m : registry.getAll()) {
                if (!m.isEnabled()) continue;
                try {
                    ActionResult result = m.onAttackEntity(cp, entity);
                    if (result != ActionResult.PASS) return result;
                } catch (Exception e) { LOGGER.error("[Noctra] {} crashed in onAttackEntity", m.getId(), e); }
            }
            return ActionResult.PASS;
        });
    }
}
