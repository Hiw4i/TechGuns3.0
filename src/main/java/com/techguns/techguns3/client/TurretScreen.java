package com.techguns.techguns3.client;

import com.techguns.techguns3.turret.TurretMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Original turret panel with server-controlled targeting switches and synced status. */
public final class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            "techguns3", "textures/gui/turret_base.png");
    private Button pvpButton;
    private Button animalsButton;

    public TurretScreen(TurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 250, 166);
        inventoryLabelY = 73;
        titleLabelX = 7;
        titleLabelY = 5;
    }

    @Override protected void init() {
        super.init();
        pvpButton = addRenderableWidget(Button.builder(buttonLabel("pvp", menu.setting(0)), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 178, topPos + 30, 67, 19).build());
        animalsButton = addRenderableWidget(Button.builder(buttonLabel("animals", menu.setting(1)), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }).bounds(leftPos + 178, topPos + 52, 67, 19).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        pvpButton.setMessage(buttonLabel("pvp", menu.setting(0)));
        animalsButton.setMessage(buttonLabel("animals", menu.setting(1)));
    }

    private static Component buttonLabel(String key, int enabled) {
        return Component.translatable("gui.techguns3.turret." + key)
                .append(": ").append(Component.translatable(
                        enabled != 0 ? "gui.techguns3.turret.on" : "gui.techguns3.turret.off"));
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
                0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.techguns3.turret.input"), 17, 6, 0x303030);
        graphics.text(font, Component.translatable("gui.techguns3.turret.output"), 115, 6, 0x303030);
        graphics.text(font, Component.translatable("gui.techguns3.turret.targets"), 179, 15, 0xFFFFFF);
        graphics.text(font, Component.translatable("gui.techguns3.turret.loaded", menu.setting(2)),
                179, 92, 0xFFFFFF);
        graphics.text(font, Component.translatable("gui.techguns3.turret.health", menu.setting(3)),
                179, 105, 0xFFFFFF);
        graphics.text(font, Component.translatable(menu.setting(4) != 0
                ? "gui.techguns3.turret.disabled_powered" : "gui.techguns3.turret.active"),
                179, 118, 0xFFFFFF);
    }
}
