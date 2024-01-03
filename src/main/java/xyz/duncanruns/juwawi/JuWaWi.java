package xyz.duncanruns.juwawi;

import com.google.common.io.Resources;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;

import java.io.IOException;
import java.nio.charset.Charset;

public class JuWaWi implements PluginInitializer {
    public static void main(String[] args) throws IOException {
        JultiAppLaunch.launchWithDevPlugin(args, PluginManager.JultiPluginData.fromString(
                Resources.toString(Resources.getResource(JuWaWi.class, "/julti.plugin.json"), Charset.defaultCharset())
        ), new JuWaWi());
    }

    @Override
    public void initialize() {
    }

    @Override
    public String getMenuButtonName() {
        return "Open Wall Window";
    }

    @Override
    public void onMenuButtonPress() {
    }
}
