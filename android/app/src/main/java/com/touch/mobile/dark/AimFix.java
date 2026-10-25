package com.touch.mobile.dark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Widescreen crosshair correction (same idea as the "crosshair position correction"
 * option of other SA-MP launchers).
 *
 * GTA SA draws the crosshair at (0.530, 0.400) of the screen, but the real aim direction was
 * authored for a 4:3 screen. On a wider screen the real hit point moves towards the screen
 * centre by the factor (4/3) / aspect. The corrected position is computed from the real
 * screen aspect ratio (so it is right on every phone) and written into
 * CCamera::m_f3rdPersonCHairMultX/Y inside libGTASA.so.
 *
 * Everything is fixed in the source - players cannot change it.
 * Fine tuning (needs a rebuild):
 *   X_ADJUST : added to the horizontal position (negative = more left)
 *   Y_ADJUST : added to the vertical position   (positive = lower, negative = higher)
 *
 * Diagnostics are written to <files>/SAMP/aimfix_status.txt (info only, safe to delete).
 */
public class AimFix {
    private static final float VANILLA_X = 0.530f;
    private static final float VANILLA_Y = 0.400f;
    private static final float X_ADJUST = -0.010f;
    private static final float Y_ADJUST = 0.030f;

    // Symbol offsets inside libGTASA.so (from its dynamic symbol table)
    private static final long OFF_MULT_Y = 0x8B07F8L; // CCamera::m_f3rdPersonCHairMultY
    private static final long OFF_MULT_X = 0x8B07FCL; // CCamera::m_f3rdPersonCHairMultX

    private static final long POLL_MS = 20; // re-apply often, in case the game resets the values

    private static boolean started = false;

    // ---- raw memory access: sun.misc.Unsafe first, /proc/self/mem as fallback ----
    private static Object unsafe;
    private static Method uGetFloat, uPutFloat;
    private static RandomAccessFile mem;

    private static boolean initUnsafe() {
        try {
            Class<?> c = Class.forName("sun.misc.Unsafe");
            Field f = null;
            for (String n : new String[]{"THE_ONE", "theUnsafe"}) {
                try { f = c.getDeclaredField(n); break; } catch (NoSuchFieldException ignored) {}
            }
            if (f == null) return false;
            f.setAccessible(true);
            unsafe = f.get(null);
            uGetFloat = c.getMethod("getFloat", long.class);
            uPutFloat = c.getMethod("putFloat", long.class, float.class);
            return unsafe != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static float getFloat(long addr) throws Exception {
        if (unsafe != null) return (Float) uGetFloat.invoke(unsafe, addr);
        byte[] b = new byte[4];
        mem.seek(addr);
        mem.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private static void putFloat(long addr, float v) throws Exception {
        if (unsafe != null) { uPutFloat.invoke(unsafe, addr, v); return; }
        byte[] b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v).array();
        mem.seek(addr);
        mem.write(b);
    }

    public static synchronized void start(final File filesDir) {
        if (started) return;
        started = true;
        final Thread t = new Thread(() -> run(filesDir), "AimFix");
        t.setDaemon(true);
        t.start();
    }

    private static void run(File filesDir) {
        File status = null;
        try {
            if (filesDir != null) {
                File dir = new File(filesDir, "SAMP");
                if (!dir.exists()) dir.mkdirs();
                status = new File(dir, "aimfix_status.txt");
            }
        } catch (Throwable ignored) {}

        String method;
        if (initUnsafe()) {
            method = "unsafe";
        } else {
            method = "procmem";
            try {
                mem = new RandomAccessFile("/proc/self/mem", "rw");
            } catch (Throwable e) {
                write(status, "error: no memory access (unsafe failed, /proc/self/mem: " + e + ")");
                return;
            }
        }

        long base = 0;
        String lastStatus = "";
        int writes = 0;

        while (true) {
            String msg;
            try {
                if (base == 0) base = findLibBase("libGTASA.so");
                if (base == 0) {
                    msg = "waiting: libGTASA base not found";
                } else {
                    float curX = getFloat(base + OFF_MULT_X);
                    float curY = getFloat(base + OFF_MULT_Y);
                    // only touch memory once the game has initialised sane values
                    if (curX > 0.05f && curX < 0.95f && curY > 0.05f && curY < 0.95f) {
                        float[] t = computeTarget();
                        float aimX = t[0], aimY = t[1];
                        if (curX != aimX) { putFloat(base + OFF_MULT_X, aimX); writes++; }
                        if (curY != aimY) { putFloat(base + OFF_MULT_Y, aimY); writes++; }
                        msg = String.format(Locale.US, "ok (%s) x=%.4f y=%.4f  screen %.0fx%.0f aspect %.3f",
                                method, aimX, aimY, t[2], t[3], t[4]);
                    } else {
                        msg = String.format(Locale.US, "waiting: unexpected values x=%.4f y=%.4f", curX, curY);
                    }
                }
            } catch (Throwable e) {
                msg = "error (" + method + "): " + e;
            }

            if (!msg.equals(lastStatus)) write(status, msg);
            lastStatus = msg;

            try { Thread.sleep(POLL_MS); } catch (InterruptedException e) { return; }
        }
    }

    /** returns {x, y, width, height, aspect} */
    private static float[] computeTarget() {
        float w = 0, h = 0;
        try {
            android.app.Activity a = com.touch.mobile.dark.gui.util.Utils.currentContext;
            if (a != null) {
                android.view.View v = a.getWindow().getDecorView();
                w = v.getWidth();
                h = v.getHeight();
            }
        } catch (Throwable ignored) {}
        if (w <= 0 || h <= 0) {
            android.util.DisplayMetrics dm = android.content.res.Resources.getSystem().getDisplayMetrics();
            w = dm.widthPixels;
            h = dm.heightPixels;
        }
        if (h > w) { float tmp = w; w = h; h = tmp; } // the game is always landscape
        float aspect = w / h;
        float f = aspect > 4f / 3f ? (4f / 3f) / aspect : 1f;
        float x = 0.5f + (VANILLA_X - 0.5f) * f + X_ADJUST;
        float y = 0.5f + (VANILLA_Y - 0.5f) * f + Y_ADJUST;
        return new float[] { x, y, w, h, aspect };
    }

    private static void write(File f, String text) {
        if (f == null) return;
        try {
            FileWriter w = new FileWriter(f);
            w.write(text + "\n");
            w.close();
        } catch (Throwable ignored) {}
    }

    private static long findLibBase(String name) throws Exception {
        BufferedReader r = new BufferedReader(new FileReader("/proc/self/maps"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.endsWith(name)) continue;
                String[] p = line.split("\\s+");
                if (p.length >= 3 && Long.parseLong(p[2], 16) == 0L) {
                    return Long.parseLong(p[0].substring(0, p[0].indexOf('-')), 16);
                }
            }
        } finally {
            r.close();
        }
        return 0;
    }
                }
