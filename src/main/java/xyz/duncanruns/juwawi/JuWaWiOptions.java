package xyz.duncanruns.juwawi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class JuWaWiOptions {
    private static final Path OPTIONS_PATH = JultiOptions.getJultiDir().resolve("juwawi.json");
    private final static Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = false;
    public List<Byte> updatePercents = Arrays.asList((byte) 5, (byte) 15);
    public boolean useMainMonitor = true;
    public int x = 0;
    public int y = 0;
    public int w = 1920;
    public int h = 1080;
    public boolean showLocks = true;
    public int lockedBorderThickness = 10;

    public static JuWaWiOptions load() {
        if (Files.exists(OPTIONS_PATH)) {
            try {
                return new Gson().fromJson(FileUtil.readString(OPTIONS_PATH), JuWaWiOptions.class);
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to load JuWaWi options:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
        return new JuWaWiOptions();
    }

    public void save() {
        try {
            FileUtil.writeString(OPTIONS_PATH, GSON_WRITER.toJson(this));
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to save JuWaWi options:\n" + ExceptionUtil.toDetailedString(e));
        }
    }
}
