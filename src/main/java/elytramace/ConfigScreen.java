package elytramace;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("ElytraMace"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.INSTANCE;
        int w = 220, x = this.width / 2 - w / 2, y = 30, step = 24;
        toggle(x, y, w, "Enabled", () -> c.enabled, v -> c.enabled = v); y += step;
        toggle(x, y, w, "legitunequipelytra", () -> c.legitUnequipElytra, v -> c.legitUnequipElytra = v); y += step;
        toggle(x, y, w, "Elytra rocket racing", () -> c.rocketRacing, v -> c.rocketRacing = v); y += step;
        toggle(x, y, w, "Auto mace hit", () -> c.autoMace, v -> c.autoMace = v); y += step;
        slider(x, y, w, "Range", 2.0, 6.0, 0.1, c.range, v -> c.range = v); y += step;
        slider(x, y, w, "Min height", 0.0, 10.0, 0.5, c.minHeight, v -> c.minHeight = v); y += step;
        toggle(x, y, w, "speardoubletap", () -> c.spearDoubleTap, v -> c.spearDoubleTap = v); y += step;
        slider(x, y, w, "speardoubletapchance %", 0, 100, 1, c.spearDoubleTapChance, v -> c.spearDoubleTapChance = (int) Math.round(v)); y += step + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y, w, 20).build());
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        this.addRenderableWidget(Button.builder(text(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(text(label, get.getAsBoolean()));
            ModConfig.save();
        }).bounds(x, y, w, 20).build());
    }

    private static Component text(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private void slider(int x, int y, int w, String label, double min, double max, double step, double cur, DoubleConsumer set) {
        this.addRenderableWidget(new Slider(x, y, w, label, min, max, step, cur, set));
    }

    private static class Slider extends AbstractSliderButton {
        private final String label;
        private final double min, max, step;
        private final DoubleConsumer set;

        Slider(int x, int y, int w, String label, double min, double max, double step, double cur, DoubleConsumer set) {
            super(x, y, w, 20, Component.empty(), (cur - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.step = step; this.set = set;
            updateMessage();
        }

        private double real() {
            double v = min + (max - min) * this.value;
            return Math.round(v / step) * step;
        }

        @Override
        protected void updateMessage() {
            double r = real();
            String s = (step >= 1) ? String.valueOf((int) r) : String.format("%.1f", r);
            setMessage(Component.literal(label + ": " + s));
        }

        @Override
        protected void applyValue() {
            set.accept(real());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        ModConfig.save();
    }
}
