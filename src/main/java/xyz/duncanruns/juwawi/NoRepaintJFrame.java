package xyz.duncanruns.juwawi;

import com.sun.jna.platform.win32.Shell32;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Way easier than using a user32.h CreateWindowEx (no making window classes or managing window messages etc etc)
 */
public class NoRepaintJFrame extends JFrame {

    public NoRepaintJFrame() {
        // No super() because we don't really want or need anything that it'll do
        super();
        this.setIgnoreRepaint(true);
    }

    @Override
    public void repaint(long time, int x, int y, int width, int height) {
        Path path = Paths.get("");
        Shell32.INSTANCE.ShellExecute(null, "open", path.toString(), null, path.getParent().toString(), 1);
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void paintAll(Graphics g) {
    }

    @Override
    public void repaint() {
    }

    @Override
    public void repaint(long tm) {
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
    }

    @Override
    public void paintComponents(Graphics g) {
    }
}
