package xyz.duncanruns.juwawi.util;

import java.awt.*;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int getRed(int winColor) {
        return winColor & 0xff;
    }

    public static int getGreen(int winColor) {
        return (winColor >> 8) & 0xff;
    }

    public static int getBlue(int winColor) {
        return (winColor >> 16) & 0xff;
    }

    public static Color fromWinColor(int winColor) {
        return new Color(getRed(winColor), getBlue(winColor), getGreen(winColor));
    }

    public static int toWinColor(Color color) {
        return color.getRed() + (color.getGreen() << 8) + (color.getBlue() << 16);
    }
}
