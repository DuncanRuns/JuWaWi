package xyz.duncanruns.juwawi.window;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.resetting.ResetManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.win32.Msimg32;
import xyz.duncanruns.juwawi.JuWaWiPlugin;
import xyz.duncanruns.juwawi.gui.JuWaWiConfigGUI;
import xyz.duncanruns.juwawi.util.ColorUtil;
import xyz.duncanruns.juwawi.win32.GDI32Extra;
import xyz.duncanruns.juwawi.win32.User32Extra;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class JuWaWi extends NoRepaintJFrame {
    private static final HBRUSH DC_BRUSH = new HBRUSH(GDI32Extra.INSTANCE.GetStockObject(18));

    private final List<Rectangle> clearScreenRequestList;
    private final Executor drawExecutor = Executors.newSingleThreadExecutor();

    // Configuration
    private final int width, height;

    // State
    private final Queue<MinecraftInstance> toUpdate = new ConcurrentLinkedQueue<>();
    private boolean shouldRefresh = false;
    private boolean paused = false;

    // Tracking
    private final Map<MinecraftInstance, Byte> lastInstancePercentages = new HashMap<>();
    private final Queue<Pair<MinecraftInstance, Long>> scheduleUpdates = new ConcurrentLinkedQueue<>();
    private List<Rectangle> lastPositions = null;

//    private final HDC lockIcon = null;
//    private int lockWidth, lockHeight;

    private boolean closed = false;
    private HWND hwnd;
    private HDC bufferHDC;

    public JuWaWi(int x, int y, int width, int height) {
        super();
        this.width = width;
        this.height = height;
        this.setupWindow();
        this.setLocation(x, y);
        this.finishSetup();
        this.clearScreenRequestList = Collections.singletonList(new Rectangle(0, 0, this.width, this.height));
    }

    public static List<Rectangle> getCurrentInstancePositions(Dimension size) {
        final ResetManager manager = ResetHelper.getManager();
        return InstanceManager.getInstanceManager().getInstances().stream().map(instance -> manager.getInstancePosition(instance, size)).collect(Collectors.toList());
    }

    private static RECT convertRectangle(Rectangle rectangle) {
        RECT winRect = new RECT();
        winRect.left = rectangle.x;
        winRect.right = rectangle.x + rectangle.width;
        winRect.top = rectangle.y;
        winRect.bottom = rectangle.y + rectangle.height;
        return winRect;
    }

    public void tick() {
        List<Rectangle> currentInstancePositions = getCurrentInstancePositions(new Dimension(this.width, this.height));

        if (ActiveWindowManager.isWallActive() && this.paused) {
            this.paused = false;
            this.shouldRefresh = true;
        } else if (!ActiveWindowManager.isWallActive() && !this.paused) {
            this.paused = true;
        }

        if (this.paused) {
            return;
        }

        if (this.shouldRefresh || this.lastPositions == null || currentInstancePositions.size() != this.lastPositions.size()) {
            this.refresh(currentInstancePositions);
            return;
        }

        this.checkSchedule();

        List<InstanceDrawRequest> toDraw = new ArrayList<>();
        List<Rectangle> toBG = new ArrayList<>();

        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
        List<MinecraftInstance> locked = ResetHelper.getManager().getLockedInstances();
        // Draw instance movements
        for (int i = 0; i < currentInstancePositions.size(); i++) {
            // Get info for this instance
            final Rectangle last = this.lastPositions.get(i);
            final Rectangle current = currentInstancePositions.get(i);
            final MinecraftInstance instance = instances.get(i);
            // Determine if instance has moved
            if (last.equals(current)) {
                continue;
            }
            // Add draw requests
            toDraw.add(new InstanceDrawRequest(instance, current, locked.contains(instance), instance.shouldCoverWithDirt()));
            toBG.add(last);
            // Prevent duplicate draw request
            this.toUpdate.removeIf(in -> in.equals(instance));
        }

        // Add draw requests from toUpdate
        this.toUpdate.forEach(instance -> toDraw.add(new InstanceDrawRequest(instance, currentInstancePositions.get(InstanceManager.getInstanceManager().getInstanceIndex(instance)), locked.contains(instance), instance.shouldCoverWithDirt())));
        this.toUpdate.clear();

        // Remove bg draws that will be covered by an instance
        toDraw.forEach(req -> toBG.remove(req.rect));

        // Remove draws outside the screen
        Rectangle screenRect = new Rectangle(0, 0, this.width, this.height);
        toDraw.removeIf(req -> !req.rect.intersects(screenRect));
        toBG.removeIf(rect -> !rect.intersects(screenRect));

        if (!(toDraw.isEmpty() && toBG.isEmpty())) {
            this.drawExecutor.execute(() -> this.draw(toDraw, toBG));
        }
        this.lastPositions = currentInstancePositions;
    }

    private void refresh(List<Rectangle> currentInstancePositions) {
        this.drawExecutor.execute(() -> this.drawAllInstances(currentInstancePositions));
        this.shouldRefresh = false;
        this.lastPositions = currentInstancePositions;
        this.lastInstancePercentages.clear();
        this.scheduleUpdates.clear();
    }

    private void checkSchedule() {
        long currentTimeMillis = System.currentTimeMillis();
        this.scheduleUpdates.removeIf(pair -> {
            long time = pair.getRight();
            if (currentTimeMillis > time) {
                this.toUpdate.add(pair.getLeft());
                return true;
            }
            return false;
        });
    }

    private void drawAllInstances(List<Rectangle> instancePositions) {
        ArrayList<InstanceDrawRequest> requests = new ArrayList<>();
        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
        List<MinecraftInstance> locked = ResetHelper.getManager().getLockedInstances();
        for (int i = 0; i < instancePositions.size(); i++) {
            MinecraftInstance instance = instances.get(i);
            requests.add(new InstanceDrawRequest(instance, instancePositions.get(i), locked.contains(instance), instance.shouldCoverWithDirt()));
        }
        this.drawExecutor.execute(() -> this.draw(requests, this.clearScreenRequestList));
    }

    public void finishSetup() {
        this.setVisible(true);
        this.tryGrabHwnd();
        while (!User32Extra.INSTANCE.IsWindowVisible(this.hwnd)) {
            sleep(5);
        }

        this.makeBufferAndFillBG();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JuWaWi.this.onClose();
            }
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 27 && e.getKeyChar() == 27) {
                    JuWaWi.this.onPressEsc();
                }
            }
        });
        ActiveWindowManager.setWallOverride(this.hwnd);
    }

    public void onClose() {
        this.closed = true;
        ActiveWindowManager.clearWallOverride();
        GDI32Extra.INSTANCE.DeleteDC(this.bufferHDC);
    }

    private void onPressEsc() {
        JuWaWiConfigGUI config = JuWaWiPlugin.openConfigGUI();
        config.setLocation(new Point(this.getX() + (this.getWidth() - config.getWidth()) / 2, this.getY() + (this.getHeight() - config.getHeight()) / 2));
    }

    private void setupWindow() {
        this.setSize(this.width, this.height);

        this.setBackground(ColorUtil.fromWinColor(JuWaWiPlugin.options.bgColor));
        this.setUndecorated(true);
        this.setResizable(false);
        this.setTitle("JuWaWi");
        this.setIconImage(JultiGUI.getLogo());
    }

    private void makeBufferAndFillBG() {
        HDC hdc = User32Extra.INSTANCE.GetDC(this.hwnd);
        if (this.bufferHDC == null) {
            this.bufferHDC = GDI32Extra.INSTANCE.CreateCompatibleDC(hdc);
            HBITMAP bufferBitmap = GDI32Extra.INSTANCE.CreateCompatibleBitmap(hdc, this.width, this.height);
            GDI32Extra.INSTANCE.SelectObject(this.bufferHDC, bufferBitmap);
        }
        RECT rect = new RECT();
        User32Extra.INSTANCE.GetClientRect(this.hwnd, rect);
        GDI32Extra.INSTANCE.SetDCBrushColor(hdc, JuWaWiPlugin.options.bgColor);
        User32Extra.INSTANCE.FillRect(hdc, rect, DC_BRUSH);
        User32Extra.INSTANCE.ReleaseDC(this.hwnd, hdc);
    }

    private void tryGrabHwnd() {
        try {
            long hwndLong = Native.getWindowID(this);
            this.hwnd = new HWND(new Pointer(hwndLong));
        } catch (Exception e) {
            Julti.log(Level.ERROR, "JuWaWi tryGrabHwnd Error: " + ExceptionUtil.toDetailedString(e));
            throw new RuntimeException(e);
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Draw instances and background colored boxes to the window
     *
     * @param requests     instance draw requests
     * @param bgRectangles areas to fill with background color
     */
    public synchronized void draw(List<InstanceDrawRequest> requests, List<Rectangle> bgRectangles) {
        // Draw directly to wall if only 1 request that isn't a locked instance, or use a buffer

        boolean useBuffer = (requests.size() + bgRectangles.size()) > 1 || requests.size() == 1 && requests.get(0).locked;
        HDC wallWindowHDC = User32Extra.INSTANCE.GetDC(this.hwnd);
        HDC drawHDC;
        if (useBuffer) {
            // Make buffer a copy of current window
            GDI32Extra.INSTANCE.BitBlt(this.bufferHDC, 0, 0, this.width, this.height, wallWindowHDC, 0, 0, GDI32Extra.SRCCOPY);
            drawHDC = this.bufferHDC;
        } else {
            drawHDC = wallWindowHDC;
        }

        // Fill bg rectangles
        GDI32Extra.INSTANCE.SetDCBrushColor(drawHDC, JuWaWiPlugin.options.bgColor);
        bgRectangles.stream().map(JuWaWi::convertRectangle).forEach(rect -> User32Extra.INSTANCE.FillRect(drawHDC, rect, DC_BRUSH));

        // Draw instances
        for (InstanceDrawRequest request : requests) {
            Rectangle destRectangle = request.rect;
            if (request.dirtCover) {
                GDI32Extra.INSTANCE.SetDCBrushColor(drawHDC, JuWaWiPlugin.options.dirtColor);
                User32Extra.INSTANCE.FillRect(drawHDC, convertRectangle(destRectangle), DC_BRUSH);
            } else {
                HDC instanceHDC = User32Extra.INSTANCE.GetDC(request.instance.getHwnd());
                RECT sourceRect = new RECT();
                User32Extra.INSTANCE.GetClientRect(request.instance.getHwnd(), sourceRect);
                Msimg32.INSTANCE.TransparentBlt(drawHDC, destRectangle.x, destRectangle.y, destRectangle.width, destRectangle.height, instanceHDC, 0, 0, sourceRect.right - sourceRect.left, sourceRect.bottom - sourceRect.top, new UINT(GDI32Extra.SRCCOPY));
                User32Extra.INSTANCE.ReleaseDC(request.instance.getHwnd(), instanceHDC);
            }
            if (request.locked && JuWaWiPlugin.options.showLocks && JuWaWiPlugin.options.lockedBorderThickness > 0) {
                final int x = destRectangle.x, w = destRectangle.width, b = JuWaWiPlugin.options.lockedBorderThickness, y = destRectangle.y, h = destRectangle.height;
                GDI32Extra.INSTANCE.SetDCBrushColor(drawHDC, JuWaWiPlugin.options.lockColor);
                User32Extra.INSTANCE.FillRect(drawHDC, convertRectangle(new Rectangle(x, y, b, h)), DC_BRUSH);
                User32Extra.INSTANCE.FillRect(drawHDC, convertRectangle(new Rectangle(x + b, y, w - (2 * b), b)), DC_BRUSH);
                User32Extra.INSTANCE.FillRect(drawHDC, convertRectangle(new Rectangle(x + w - b, y, b, h)), DC_BRUSH);
                User32Extra.INSTANCE.FillRect(drawHDC, convertRectangle(new Rectangle(x + b, y + h - b, w - (2 * b), b)), DC_BRUSH);
            }
        }

        if (useBuffer) {
            // Copy pixels from buffer to main window if using buffer
            GDI32Extra.INSTANCE.BitBlt(wallWindowHDC, 0, 0, this.width, this.height, drawHDC, 0, 0, GDI32Extra.SRCCOPY);
        }


        User32Extra.INSTANCE.ReleaseDC(this.hwnd, wallWindowHDC);
    }

    public void onAllInstancesFound() {
        this.shouldRefresh = true;
    }

    public void onInstancePercentageChange(MinecraftInstance instance) {
//        if(true)return;
        if (!instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING)) {
            return;
        }
        byte lastPercent = this.lastInstancePercentages.getOrDefault(instance, (byte) 0);
        byte newPercent = instance.getStateTracker().getLoadingPercent();
        JuWaWiPlugin.options.updatePercents.forEach(updatePoint -> {
            if (lastPercent < updatePoint && newPercent >= updatePoint) {
                this.toUpdate.add(instance);
            }
        });
        this.lastInstancePercentages.put(instance, newPercent);
    }

    public void onInstanceReset(MinecraftInstance instance) {
        this.unscheduleUpdates(instance);
        if (JultiOptions.getJultiOptions().doDirtCovers) {
            this.toUpdate.add(instance);
        } else {
            this.scheduleUpdate(instance, 100);
        }
    }

    public void onInstanceStateChange(MinecraftInstance instance) {
        switch (instance.getStateTracker().getInstanceState()) {
            case WAITING:
                this.unscheduleUpdates(instance);
                if (JultiOptions.getJultiOptions().doDirtCovers) {
                    this.toUpdate.add(instance);
                } else {
                    this.scheduleUpdate(instance, 100);
                }
            case GENERATING:
                this.unscheduleUpdates(instance);
                if (JultiOptions.getJultiOptions().doDirtCovers) {
                    this.toUpdate.add(instance);
                } else {
                    this.toUpdate.add(instance);
                }
                break;
            case PREVIEWING:
                this.unscheduleUpdates(instance);
                this.scheduleUpdate(instance, 100);
                break;
            case INWORLD:
                this.unscheduleUpdates(instance);
                this.scheduleUpdate(instance, 100);
                this.scheduleUpdate(instance, 1000);
                this.scheduleUpdate(instance, 2000);
                break;
        }
    }

    private void scheduleUpdate(MinecraftInstance instance, int millis) {
        this.scheduleUpdates.add(Pair.of(instance, System.currentTimeMillis() + millis));
    }

    private void unscheduleUpdates(MinecraftInstance instance) {
        this.scheduleUpdates.removeIf(pair -> pair.getLeft().equals(instance));
    }

    public void onInstanceLock(MinecraftInstance instance) {
        if (JuWaWiPlugin.options.showLocks) {
            this.toUpdate.add(instance);
        }
    }

    public void onWallActivate() {
        this.paused = false;
        this.shouldRefresh = true;
    }

    public static class InstanceDrawRequest {
        private final MinecraftInstance instance;
        private final Rectangle rect;
        private final boolean locked;
        private final boolean dirtCover;

        /**
         * @param instance  the minecraft instance to draw
         * @param rectangle the specified rectangle to draw on the wall window
         */
        public InstanceDrawRequest(MinecraftInstance instance, Rectangle rectangle, boolean locked, boolean dirtCover) {
            this.instance = instance;
            this.rect = rectangle;
            this.locked = locked;
            this.dirtCover = dirtCover;
        }
    }
}
