package xyz.duncanruns.juwawi;

import com.google.common.io.Resources;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.juwawi.command.SetPreviewPercentsCommand;
import xyz.duncanruns.juwawi.gui.JuWaWiConfigGUI;
import xyz.duncanruns.juwawi.window.JuWaWi;

import java.io.IOException;
import java.nio.charset.Charset;

public class JuWaWiPlugin implements PluginInitializer {
    private static JuWaWi juwawi = null;
    private static JuWaWiConfigGUI configGUI = null;

    public static JuWaWiOptions options;

    public static void main(String[] args) throws IOException {
        JultiAppLaunch.launchWithDevPlugin(args, PluginManager.JultiPluginData.fromString(
                Resources.toString(Resources.getResource(JuWaWiPlugin.class, "/julti.plugin.json"), Charset.defaultCharset())
        ), new JuWaWiPlugin());
    }

    public static JuWaWi getWallWindow() {
        return juwawi;
    }

    public static synchronized JuWaWi openWallWindow() {
        if (wallWindowExists()) {
            juwawi.requestFocus();
            return juwawi;
        }
        return createWallWindow();
    }

    public static JuWaWiConfigGUI openConfigGUI() {
        if (configGUI == null || configGUI.isClosed()) {
            configGUI = new JuWaWiConfigGUI();
            return configGUI;
        }
        configGUI.requestFocus();
        return configGUI;
    }

    private static JuWaWi createWallWindow() {
        if (options.useMainMonitor) {
            MonitorUtil.Monitor m = MonitorUtil.getPrimaryMonitor();
            juwawi = new JuWaWi(m.x, m.y, m.width, m.height);
        } else {
            juwawi = new JuWaWi(options.x, options.y, options.w, options.h);
        }
        return juwawi;
    }

    private static boolean wallWindowExists() {
        return juwawi != null && !getWallWindow().isClosed();
    }

    public static void refreshWallWindow() {
        if (wallWindowExists()) {
            juwawi.dispose();
            juwawi.onClose();
        }
    }

    private static synchronized void tick() {
        if (options == null) {
            return;
        }
        if (options.enabled && !wallWindowExists()) {
            openWallWindow();
        } else if (!options.enabled && wallWindowExists()) {
            juwawi.dispose();
            juwawi.onClose();
        }
        if (wallWindowExists()) {
            juwawi.tick();
        }
    }

    @Override
    public void initialize() {
        PluginEvents.RunnableEventType.ALL_INSTANCES_FOUND.register(() -> {
            if (wallWindowExists()) {
                juwawi.onAllInstancesFound();
            }
        });
        PluginEvents.InstanceEventType.PERCENTAGE_CHANGE.register(instance -> {
            if (wallWindowExists()) {
                juwawi.onInstancePercentageChange(instance);
            }
        });
        PluginEvents.InstanceEventType.RESET.register(instance -> {
            if (wallWindowExists()) {
                juwawi.onInstanceReset(instance);
            }
        });
        PluginEvents.InstanceEventType.STATE_CHANGE.register(instance -> {
            if (wallWindowExists()) {
                juwawi.onInstanceStateChange(instance);
            }
        });
        PluginEvents.InstanceEventType.LOCK.register(instance -> {
            if (wallWindowExists()) {
                juwawi.onInstanceLock(instance);
            }
        });
        PluginEvents.RunnableEventType.WALL_ACTIVATE.register(() -> {
            if (wallWindowExists()) {
                juwawi.onWallActivate();
            }
        });
        PluginEvents.RunnableEventType.END_TICK.register(JuWaWiPlugin::tick);

        // Register Commands
        CommandManager.getMainManager().registerCommand(new SetPreviewPercentsCommand());

        // Register data savers
        JultiOptions.registerPluginDataLoader("juwawi", data -> options = JuWaWiOptions.fromJsonObject(data));
        JultiOptions.registerPluginDataSaver("juwawi", () -> options.asJsonObject());
    }

    @Override
    public String getMenuButtonName() {
        return "Open Config";
    }

    @Override
    public synchronized void onMenuButtonPress() {
        Point location = JultiGUI.getPluginsGUI().getLocation();
        openConfigGUI().setLocation(location.x, location.y + 30);
    }
}
