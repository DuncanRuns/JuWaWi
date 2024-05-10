package xyz.duncanruns.juwawi.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.juwawi.JuWaWiPlugin;
import xyz.duncanruns.juwawi.util.ColorUtil;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

public class JuWaWiConfigGUI extends JFrame {
    private boolean closed = false;
    private JScrollPane scrollPane = null;

    public JuWaWiConfigGUI() {
        this.setupWindow();
        this.reload();
        this.setVisible(true);
    }

    private static JPanel getWindowPosPanel() {
        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new BoxLayout(positionPanel, BoxLayout.X_AXIS));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField xField = new JFormattedTextField(formatter);
        JFormattedTextField yField = new JFormattedTextField(formatter);
        GUIUtil.setActualSize(xField, 50, 23);
        GUIUtil.setActualSize(yField, 50, 23);
        positionPanel.add(xField);
        positionPanel.add(yField);
        xField.setValue(JuWaWiPlugin.options.x);
        yField.setValue(JuWaWiPlugin.options.y);
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                Julti.waitForExecute(() -> {
                    JuWaWiPlugin.options.x = (int) xField.getValue();
                    JuWaWiPlugin.options.y = (int) yField.getValue();
                });
            }
        };
        xField.addKeyListener(keyListener);
        yField.addKeyListener(keyListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }

    private static JPanel getWindowSizePanel() {
        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new BoxLayout(positionPanel, BoxLayout.X_AXIS));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField xField = new JFormattedTextField(formatter);
        JFormattedTextField yField = new JFormattedTextField(formatter);
        GUIUtil.setActualSize(xField, 50, 23);
        GUIUtil.setActualSize(yField, 50, 23);
        positionPanel.add(xField);
        positionPanel.add(yField);
        xField.setValue(JuWaWiPlugin.options.w);
        yField.setValue(JuWaWiPlugin.options.h);
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                Julti.waitForExecute(() -> {
                    JuWaWiPlugin.options.w = (int) xField.getValue();
                    JuWaWiPlugin.options.h = (int) yField.getValue();
                });
            }
        };
        xField.addKeyListener(keyListener);
        yField.addKeyListener(keyListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }

    private static JSlider getBorderThicknessSlider(JLabel borderThicknessLabel) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, 100, JuWaWiPlugin.options.lockedBorderThickness);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Julti.waitForExecute(() -> {
                    JuWaWiPlugin.options.lockedBorderThickness = slider.getValue();
                });
                borderThicknessLabel.setText("Lock Border Thickness (" + JuWaWiPlugin.options.lockedBorderThickness + "):");
            }
        });
        return slider;
    }

    private void reload() {
        int scrollX = 0, scrollY = 0;
        if (this.scrollPane != null) {
            scrollX = this.scrollPane.getHorizontalScrollBar().getValue();
            scrollY = this.scrollPane.getVerticalScrollBar().getValue();
        }

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        this.scrollPane = new JScrollPane(panel);
        this.scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        this.setContentPane(this.scrollPane);

        this.addComponents(panel);

        this.scrollPane.getHorizontalScrollBar().setValue(scrollX);
        this.scrollPane.getVerticalScrollBar().setValue(scrollY);
        this.revalidate();
    }

    private void addComponents(JPanel panel) {
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBox("Enabled", "", JuWaWiPlugin.options.enabled, b -> {
            Julti.waitForExecute(() -> {
                JuWaWiPlugin.options.enabled = b;
            });
            this.reload();

        })));
        if (!JuWaWiPlugin.options.enabled) return;
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBox("Use Main Monitor", "", JuWaWiPlugin.options.useMainMonitor, b -> {
            Julti.waitForExecute(() -> {
                JuWaWiPlugin.options.useMainMonitor = b;
            });
            this.reload();
        })));
        if (!JuWaWiPlugin.options.useMainMonitor) {
            this.addWindowSettings(panel);
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBox("Show Locks", "", JuWaWiPlugin.options.showLocks, b -> {
            Julti.waitForExecute(() -> {
                JuWaWiPlugin.options.showLocks = b;
            });
            this.reload();
        })));

        if (JuWaWiPlugin.options.showLocks) {
            panel.add(GUIUtil.createSpacer());

            panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Choose Lock Color"), e -> {
                int c = JuWaWiPlugin.options.lockColor;
                Color newColor = JColorChooser.showDialog(panel, "JuWaWi: Choose Lock Color", ColorUtil.fromWinColor(c));
                if (newColor == null) {
                    return;
                }
                Julti.waitForExecute(() -> {
                    JuWaWiPlugin.options.lockColor = ColorUtil.toWinColor(newColor);
                });
            })));
            panel.add(GUIUtil.createSpacer());
            JLabel borderThicknessLabel = new JLabel("Lock Border Thickness (" + JuWaWiPlugin.options.lockedBorderThickness + "):");
            panel.add(GUIUtil.leftJustify(borderThicknessLabel));
            JSlider slider = getBorderThicknessSlider(borderThicknessLabel);
            GUIUtil.setActualSize(slider, 200, 23);
            panel.add(GUIUtil.leftJustify(slider));
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Choose Dirt Cover Color"), e -> {
            int c = JuWaWiPlugin.options.dirtColor;
            Color newColor = JColorChooser.showDialog(panel, "JuWaWi: Choose Dirt Cover Color", ColorUtil.fromWinColor(c));
            if (newColor == null) {
                return;
            }
            Julti.waitForExecute(() -> {
                JuWaWiPlugin.options.dirtColor = ColorUtil.toWinColor(newColor);
            });
        })));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Choose Background Color"), e -> {
            int c = JuWaWiPlugin.options.bgColor;
            Color newColor = JColorChooser.showDialog(panel, "JuWaWi: Choose Background Color", ColorUtil.fromWinColor(c));
            if (newColor == null) {
                return;
            }
            Julti.waitForExecute(() -> {
                JuWaWiPlugin.options.bgColor = ColorUtil.toWinColor(newColor);
            });
        })));
    }

    private void addWindowSettings(JPanel panel) {
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Window Position")));
        panel.add(GUIUtil.leftJustify(getWindowPosPanel()));
        panel.add(GUIUtil.leftJustify(new JLabel("Window Size")));
        panel.add(GUIUtil.leftJustify(getWindowSizePanel()));
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("JuWaWi Config");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JuWaWiConfigGUI.this.onClose();
            }
        });
        this.setSize(340, 400);
    }

    private void onClose() {
        // Close wall window to refresh
        JuWaWiPlugin.refreshWallWindow();
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
