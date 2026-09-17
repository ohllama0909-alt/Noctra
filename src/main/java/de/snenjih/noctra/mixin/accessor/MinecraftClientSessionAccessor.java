package de.snenjih.noctra.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionAccessor {

    @Mutable
    @Accessor("session")
    void setSession(Session session);

    @Accessor("session")
    Session getSession();
}
