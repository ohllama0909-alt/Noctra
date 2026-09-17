package de.snenjih.noctra.mixin;

import de.snenjih.noctra.menu.MainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Shadow private ButtonWidget exitButton;

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void addNoctraButton(CallbackInfo ci) {
        int oldY = exitButton.getY();
        exitButton.setY(oldY + 28);
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Noctra"),
            btn -> MinecraftClient.getInstance().setScreen(new MainMenuScreen((Screen) (Object) this))
        ).dimensions(exitButton.getX(), oldY, exitButton.getWidth(), exitButton.getHeight()).build());
    }
}
