package xyz.duncanruns.juwawi;

import com.google.common.io.Resources;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.juwawi.window.JuWaWi;

import java.io.IOException;
import java.nio.charset.Charset;

public class JuWaWiPlugin implements PluginInitializer {
    private static JuWaWi juwawi = null;
    private static boolean dev = false;

    public static JuWaWiOptions options;

    public static void main(String[] args) throws IOException {
        JuWaWiPlugin.dev = true;
        JultiAppLaunch.launchWithDevPlugin(args, PluginManager.JultiPluginData.fromString(
                Resources.toString(Resources.getResource(JuWaWiPlugin.class, "/julti.plugin.json"), Charset.defaultCharset())
        ), new JuWaWiPlugin());
    }

    public static JuWaWi getWallWindow() {
        return JuWaWiPlugin.juwawi;
    }

    public static synchronized JuWaWi openWallWindow() {
        if (JuWaWiPlugin.juwawi != null && !JuWaWiPlugin.juwawi.isClosed()) {
            JuWaWiPlugin.juwawi.requestFocus();
            return JuWaWiPlugin.juwawi;
        }
        return JuWaWiPlugin.createWallWindow();
    }

    private static JuWaWi createWallWindow() {
        JuWaWiPlugin.juwawi = new JuWaWi(JuWaWiPlugin.options.x, JuWaWiPlugin.options.y, JuWaWiPlugin.options.w, JuWaWiPlugin.options.h);
        return JuWaWiPlugin.juwawi;
    }

    private static boolean wallWindowExists() {
        return JuWaWiPlugin.juwawi != null && !JuWaWiPlugin.getWallWindow().isClosed();
    }

    @Override
    public void initialize() {
        JuWaWiPlugin.options = JuWaWiOptions.load();
        JuWaWiPlugin.options.save();
        PluginEvents.RunnableEventType.END_TICK.register(() -> {
            if (JuWaWiPlugin.options.enabled && !JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.openWallWindow();
            } else if (!JuWaWiPlugin.options.enabled && JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.dispose();
            }
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.tick();
            }
        });
        PluginEvents.RunnableEventType.ALL_INSTANCES_FOUND.register(() -> {
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.onAllInstancesFound();
            }
        });
        PluginEvents.InstanceEventType.PERCENTAGE_CHANGE.register(instance -> {
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.onInstancePercentageChange(instance);
            }
        });
        PluginEvents.InstanceEventType.RESET.register(instance -> {
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.onInstanceReset(instance);
            }
        });
        PluginEvents.InstanceEventType.STATE_CHANGE.register(instance -> {
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.onInstanceStateChange(instance);
            }
        });
        PluginEvents.InstanceEventType.LOCK.register(instance -> {
            if (JuWaWiPlugin.wallWindowExists()) {
                JuWaWiPlugin.juwawi.onInstanceLock(instance);
            }
        });
    }

    @Override
    public String getMenuButtonName() {
        return "Open Wall Window";
    }

    @Override
    public void onMenuButtonPress() {
        JuWaWiPlugin.openWallWindow();
    }
}
