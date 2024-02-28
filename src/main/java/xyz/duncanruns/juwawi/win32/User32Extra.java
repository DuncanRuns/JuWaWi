package xyz.duncanruns.juwawi.win32;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public interface User32Extra extends xyz.duncanruns.julti.win32.User32 {
    User32Extra INSTANCE = Native.load("user32", User32Extra.class);

    HWND CreateWindowExA(int dwExStyle, WString lpClassName, String lpWindowName, int dwStyle, int X, int Y, int nWidth, int nHeight, HWND hWndParent, HMENU hMenu, HINSTANCE hInstance, LPVOID lpParam);

    ATOM RegisterClassExA(WinUser.WNDCLASSEX var1);

    LRESULT DefWindowProcA(HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);

    boolean GetMessageA(MSG lpMsg, HWND hwnd, int wMsgFilterMin, int wMsgFilterMax);

    boolean PeekMessageA(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg);

    LRESULT DispatchMessageA(MSG lpMsg);

    HDC BeginPaint(HWND hWnd, WinUserExtra.PAINTSTRUCT paintstruct);

    boolean EndPaint(HWND hWnd, WinUserExtra.PAINTSTRUCT paintstruct);

    int GetClassNameA(WinDef.HWND hWnd, byte[] lpClassName, int nMaxCount);
}
