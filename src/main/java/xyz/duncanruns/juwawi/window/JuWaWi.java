package xyz.duncanruns.juwawi.window;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.*;
import sun.awt.windows.WComponentPeer;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.resetting.ResetManager;
import xyz.duncanruns.julti.win32.Msimg32;
import xyz.duncanruns.juwawi.NoRepaintJFrame;
import xyz.duncanruns.juwawi.win32.GDI32Extra;
import xyz.duncanruns.juwawi.win32.User32Extra;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.peer.ComponentPeer;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class JuWaWi extends NoRepaintJFrame {
    private static final HBRUSH BLACK_BRUSH = new HBRUSH(GDI32Extra.INSTANCE.GetStockObject(4));
    private static final HBRUSH WHITE_BRUSH = new HBRUSH(GDI32Extra.INSTANCE.GetStockObject(0));
    private static final UINT WHITE = new UINT(0xFFFFFF);
    private final List<Rectangle> clearScreenRequestList;
    private final Executor drawExecutor = Executors.newSingleThreadExecutor();

    // Configuration
    private final List<Byte> percentUpdates = Arrays.asList((byte) 5, (byte) 15);
    private final int width, height;

    // State
    private final Queue<MinecraftInstance> toUpdate = new LinkedList<>();
    private boolean shouldRefresh = false;

    // Tracking
    private final Map<MinecraftInstance, Byte> lastInstancePercentages = new HashMap<>();
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

    public static List<Rectangle> getCurrentInstancePositions() {
        final ResetManager manager = ResetHelper.getManager();
        return InstanceManager.getInstanceManager().getInstances().stream().map(manager::getInstancePosition).collect(Collectors.toList());
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
        List<Rectangle> currentInstancePositions = JuWaWi.getCurrentInstancePositions();

        if (this.shouldRefresh || this.lastPositions == null || currentInstancePositions.size() != this.lastPositions.size()) {
            this.drawExecutor.execute(() -> this.drawAllInstances(currentInstancePositions));
            this.shouldRefresh = false;
            this.lastPositions = currentInstancePositions;
            return;
        }

        List<InstanceDrawRequest> toDraw = new ArrayList<>();
        List<Rectangle> toBlack = new ArrayList<>();

        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
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
            toDraw.add(new InstanceDrawRequest(instance, current));
            toBlack.add(last);
            // Prevent duplicate draw request
            this.toUpdate.remove(instance);
        }
        // Remove black draws that will be covered by an instance
        toDraw.forEach(req -> toBlack.remove(req.rect));

        // Remove draws outside the screen
        Rectangle screenRect = new Rectangle(this.width, this.height);
        toDraw.removeIf(req -> !req.rect.intersects(screenRect));
        toBlack.removeIf(rect -> !rect.intersects(screenRect));

        // Add draw requests from toUpdate
        this.toUpdate.forEach(instance -> toDraw.add(new InstanceDrawRequest(instance, currentInstancePositions.get(InstanceManager.getInstanceManager().getInstanceIndex(instance)))));
        this.toUpdate.clear();

        if (!(toDraw.isEmpty() && toBlack.isEmpty())) {
            this.drawExecutor.execute(() -> this.draw(toDraw, toBlack));
        }
        this.lastPositions = currentInstancePositions;
    }

    private void drawAllInstances(List<Rectangle> instancePositions) {
        ArrayList<InstanceDrawRequest> requests = new ArrayList<>();
        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
        for (int i = 0; i < instancePositions.size(); i++) {
            Rectangle rectangle = instancePositions.get(i);
            requests.add(new InstanceDrawRequest(instances.get(i), rectangle));
        }
        this.drawExecutor.execute(() -> this.draw(requests, this.clearScreenRequestList));
    }

    public void finishSetup() {
        this.setVisible(true);
        if (!this.tryGrabHwnd()) {
            throw new RuntimeException("Failed to open Wall Window! Could not obtain low level window handle.");
        }
        while (!User32Extra.INSTANCE.IsWindowVisible(this.hwnd)) {
            sleep(5);
        }

        this.makeBufferAndFillBlack();
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
    }

    private void onClose() {
        this.closed = true;
        GDI32Extra.INSTANCE.DeleteDC(this.bufferHDC);
    }

    private void onPressEsc() {
    }

    private void setupWindow() {
        this.setSize(this.width, this.height);

        this.setBackground(Color.BLACK);
        this.setUndecorated(true);
        this.setResizable(false);
        this.setTitle("JuWaWi");
        this.setIconImage(JultiGUI.getLogo());
    }

    private void makeBufferAndFillBlack() {
        HDC hdc = User32Extra.INSTANCE.GetDC(this.hwnd);
        if (this.bufferHDC == null) {
            this.bufferHDC = GDI32Extra.INSTANCE.CreateCompatibleDC(hdc);
            HBITMAP bufferBitmap = GDI32Extra.INSTANCE.CreateCompatibleBitmap(hdc, this.width, this.height);
            GDI32Extra.INSTANCE.SelectObject(this.bufferHDC, bufferBitmap);
        }
        RECT rect = new RECT();
        User32Extra.INSTANCE.GetClientRect(this.hwnd, rect);
        User32Extra.INSTANCE.FillRect(hdc, rect, JuWaWi.BLACK_BRUSH);
        User32Extra.INSTANCE.ReleaseDC(this.hwnd, hdc);
    }

    private boolean tryGrabHwnd() {
        try {
            // getPeer() might not exist depending on used java, but peer field will, so reflection hacks lolololol
            Field peerField = Component.class.getDeclaredField("peer");
            peerField.setAccessible(true);
            ComponentPeer peer = (ComponentPeer) peerField.get(this);
            long hwndLong = ((WComponentPeer) peer).getHWnd();
            this.hwnd = new HWND(new Pointer(hwndLong));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Draw instances and black boxes to the window
     *
     * @param requests        instance draw requests
     * @param blackRectangles areas to fill with black
     */
    public synchronized void draw(List<InstanceDrawRequest> requests, List<Rectangle> blackRectangles) {
        // Draw directly to wall if only 1 instance, or use a buffer if multiple

        boolean useBuffer = (requests.size() + blackRectangles.size()) > 1;
        HDC wallWindowHDC = User32Extra.INSTANCE.GetDC(this.hwnd);
        HDC drawHDC;
        if (useBuffer) {
            // Make buffer a copy of current window
            GDI32Extra.INSTANCE.BitBlt(this.bufferHDC, 0, 0, this.width, this.height, wallWindowHDC, 0, 0, GDI32Extra.SRCCOPY);
            drawHDC = this.bufferHDC;
        } else {
            drawHDC = wallWindowHDC;
        }

        // Fill black rectangles
        blackRectangles.stream().map(JuWaWi::convertRectangle).forEach(rect -> User32Extra.INSTANCE.FillRect(drawHDC, rect, JuWaWi.BLACK_BRUSH));

        // Draw instances
        for (InstanceDrawRequest request : requests) {
            HDC instanceHDC = User32Extra.INSTANCE.GetDC(request.instance.getHwnd());
            RECT rect = new RECT();
            User32Extra.INSTANCE.GetClientRect(request.instance.getHwnd(), rect);
            Msimg32.INSTANCE.TransparentBlt(drawHDC, request.rect.x, request.rect.y, request.rect.width, request.rect.height, instanceHDC, 0, 0, rect.right - rect.left, rect.bottom - rect.top, new UINT(GDI32Extra.SRCCOPY));
            User32Extra.INSTANCE.ReleaseDC(request.instance.getHwnd(), instanceHDC);
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
        this.percentUpdates.forEach(updatePoint -> {
            if (lastPercent < updatePoint && newPercent >= updatePoint) {
//                System.out.println(lastPercent + "->" + newPercent);
                this.toUpdate.add(instance);
            }
        });
        this.lastInstancePercentages.put(instance, newPercent);
    }

    public void onInstanceReset(MinecraftInstance instance) {
        this.toUpdate.add(instance);
    }

    public void onInstanceStateChange(MinecraftInstance instance) {
        switch (instance.getStateTracker().getInstanceState()) {
            case WAITING:
            case PREVIEWING:
            case INWORLD:
                this.toUpdate.add(instance);
                break;
        }
    }

    public static class InstanceDrawRequest {
        private final MinecraftInstance instance;
        private final Rectangle rect;


        /**
         * @param instance  the minecraft instance to draw
         * @param rectangle the specified rectangle to draw on the wall window
         */
        public InstanceDrawRequest(MinecraftInstance instance, Rectangle rectangle) {
            this.instance = instance;
            this.rect = rectangle;
        }
    }
}
