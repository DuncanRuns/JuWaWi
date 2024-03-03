package xyz.duncanruns.juwawi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

public class JuWaWiOptions {
    private final static Gson GSON = new Gson();

    public boolean enabled = false;
    public List<Byte> updatePercents = Arrays.asList((byte) 5, (byte) 15);
    public boolean useMainMonitor = true;
    public int x = 0;
    public int y = 0;
    public int w = 1920;
    public int h = 1080;
    public boolean showLocks = true;
    public int lockedBorderThickness = 10;
    public int lockColor = 0xffffff; // White
    public int dirtColor = 0x000000; // Black
    public int bgColor = 0x111111; // Dark Gray

    public static JuWaWiOptions fromJsonObject(JsonObject data) {
        return GSON.fromJson(data, JuWaWiOptions.class);
    }

    public JsonObject asJsonObject() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }
}
