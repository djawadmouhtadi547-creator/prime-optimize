package com.prime.optimize.gui;

import com.prime.optimize.PrimeOptimizeConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class PrimeConfigScreen extends Screen {
    private final Screen parent;
    private ButtonWidget frustumButton;
    private ButtonWidget lodButton;
    private ButtonWidget blockEntityButton;
    private ButtonWidget lodDistanceButton;
    private ButtonWidget blockEntityDistanceButton;

    public PrimeConfigScreen(Screen parent) {
        super(Text.literal("Prime Optimize"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int y = 48;
        int width = 260;
        int height = 20;
        int gap = 25;

        frustumButton = addDrawableChild(ButtonWidget.builder(toggleText("Frustum helpers", PrimeOptimizeConfig.enableFrustumHelpers), b -> {
            PrimeOptimizeConfig.enableFrustumHelpers = !PrimeOptimizeConfig.enableFrustumHelpers;
            b.setMessage(toggleText("Frustum helpers", PrimeOptimizeConfig.enableFrustumHelpers));
            PrimeOptimizeConfig.save();
        }).dimensions(center - width / 2, y, width, height).build());

        lodButton = addDrawableChild(ButtonWidget.builder(toggleText("Distance LOD scheduling", PrimeOptimizeConfig.enableDistanceLod), b -> {
            PrimeOptimizeConfig.enableDistanceLod = !PrimeOptimizeConfig.enableDistanceLod;
            b.setMessage(toggleText("Distance LOD scheduling", PrimeOptimizeConfig.enableDistanceLod));
            PrimeOptimizeConfig.save();
        }).dimensions(center - width / 2, y += gap, width, height).build());

        blockEntityButton = addDrawableChild(ButtonWidget.builder(toggleText("Block entity distance limit", PrimeOptimizeConfig.enableBlockEntityDistanceLimit), b -> {
            PrimeOptimizeConfig.enableBlockEntityDistanceLimit = !PrimeOptimizeConfig.enableBlockEntityDistanceLimit;
            b.setMessage(toggleText("Block entity distance limit", PrimeOptimizeConfig.enableBlockEntityDistanceLimit));
            PrimeOptimizeConfig.save();
        }).dimensions(center - width / 2, y += gap, width, height).build());

        lodDistanceButton = addDrawableChild(ButtonWidget.builder(lodDistanceText(), b -> {
            PrimeOptimizeConfig.lodDistanceChunks = PrimeOptimizeConfig.lodDistanceChunks >= 16 ? 4 : PrimeOptimizeConfig.lodDistanceChunks + 2;
            b.setMessage(lodDistanceText());
            PrimeOptimizeConfig.save();
        }).dimensions(center - width / 2, y += gap, width, height).build());

        blockEntityDistanceButton = addDrawableChild(ButtonWidget.builder(blockEntityDistanceText(), b -> {
            PrimeOptimizeConfig.maxBlockEntityDistance = PrimeOptimizeConfig.maxBlockEntityDistance >= 256 ? 64 : PrimeOptimizeConfig.maxBlockEntityDistance + 64;
            b.setMessage(blockEntityDistanceText());
            PrimeOptimizeConfig.save();
        }).dimensions(center - width / 2, y += gap, width, height).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
            .dimensions(center - 100, this.height - 28, 200, 20).build());
    }

    private static Text toggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private static Text lodDistanceText() {
        return Text.literal("LOD distance: " + PrimeOptimizeConfig.lodDistanceChunks + " chunks");
    }

    private static Text blockEntityDistanceText() {
        return Text.literal("Block entity range: " + PrimeOptimizeConfig.maxBlockEntityDistance + " blocks");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFF);
        int creditsY = height - 76;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Credits"), width / 2, creditsY, 0x66FFAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Prime  •  GMU Studios  •  djawad1"), width / 2, creditsY + 14, 0xFFFFFF);
    }

    @Override
    public void close() {
        PrimeOptimizeConfig.save();
        if (client != null) client.setScreen(parent);
    }
}
