package com.fordmc.fmutils.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

public class FMSlider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final double step;
    private final Function<Double, Component> labelFactory;
    private final Consumer<Double> onValueChanged;

    public FMSlider(int x, int y, int width, int height, double currentRealValue, double min, double max, double step, Function<Double, Component> labelFactory, Consumer<Double> onValueChanged) {
        super(x, y, width, height, Component.empty(), currentRealValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.labelFactory = labelFactory;
        this.onValueChanged = onValueChanged;
        
        this.value = Math.max(0.0D, Math.min(1.0D, (currentRealValue - min) / (max - min)));
        this.updateMessage();
    }

    public double getRealValue() {
        double raw = min + (max - min) * this.value;
        if (step > 0.0) {
            raw = Math.round(raw / step) * step;
        }
        return raw;
    }

    @Override
    protected void updateMessage() {
        if (this.labelFactory != null) {
            this.setMessage(this.labelFactory.apply(this.getRealValue()));
        }
    }

    @Override
    protected void applyValue() {
        if (this.onValueChanged != null) {
            this.onValueChanged.accept(this.getRealValue());
        }
    }
}
