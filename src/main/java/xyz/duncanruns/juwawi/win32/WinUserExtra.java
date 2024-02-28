package xyz.duncanruns.juwawi.win32;

import com.sun.jna.Structure;

public interface WinUserExtra extends com.sun.jna.platform.win32.WinUser {

    @Structure.FieldOrder({"hdc", "fErase", "rcPaint", "fRestore", "fIncUpdate", "rgbReserved"})
    class PAINTSTRUCT extends Structure {
        public HDC hdc;
        public boolean fErase;
        public RECT rcPaint;
        public boolean fRestore;
        public boolean fIncUpdate;
        public BYTE[] rgbReserved = new BYTE[32];
    }
}
