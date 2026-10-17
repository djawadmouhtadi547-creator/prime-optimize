package com.prime.optimize.gui;

import com.prime.optimize.PrimeOptimize;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public final class PrimeConfigScreen extends Screen {
    private final Screen parent;

    public PrimeConfigScreen(Screen parent) {
        super(Text.literal("Prime Optimize"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonWidth = Math.min(260, this.width - 24);
        int buttonX = centerX - buttonWidth / 2;
        int top = Math.max(44, this.height / 5);

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(PrimeOptimize.aggressiveOcclusion)
                .build(buttonX, top, buttonWidth, 20, Text.literal("Aggressive occlusion & block-entity culling"),
                        (button, value) -> {
                            PrimeOptimize.aggressiveOcclusion = value;
                            PrimeOptimize.saveConfig();
                        }));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(PrimeOptimize.lodEnabled)
                .build(buttonX, top + 26, buttonWidth, 20, Text.literal("Distant chunk LOD priority"),
                        (button, value) -> {
                            PrimeOptimize.lodEnabled = value;
                            PrimeOptimize.saveConfig();
                        }));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(PrimeOptimize.memoryOptimizations)
                .build(buttonX, top + 52, buttonWidth, 20, Text.literal("Chunk cache cleanup"),
                        (button, value) -> {
                            PrimeOptimize.memoryOptimizations = value;
                            PrimeOptimize.saveConfig();
                        }));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Prime Optimize"),
                this.width / 2, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("LOD priority distance: " + PrimeOptimize.lodDistanceChunks + " chunks"),
                this.width / 2, Math.max(126, this.height / 5 + 82), 0xB8DFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Development Credits"),
                this.width / 2, this.height - 78, 0xFFD166);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Prime  •  GMU Studios  •  djawad1"),
                this.width / 2, this.height - 62, 0xFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
