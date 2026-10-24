package com.touch.mobile.dark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Moves the 3rd-person crosshair (CCamera::m_f3rdPersonCHairMultX/Y in libGTASA.so)
 * so it sits where bullets really land.
 *
 * The values are fixed here in the source - players cannot change them.
 * To adjust: edit AIM_X / AIM_Y below and rebuild.
 *   AIM_X : horizontal position, 0..1 of the screen width  (vanilla 0.530; smaller = more left)
 *   AIM_Y : vertical position,   0..1 of the screen height (vanilla 0.400; bigger  = lower)
 *
 * Diagnostics are written to <files>/SAMP/aimfix_status.txt (read-only info, safe to delete).
 */
public class AimFix {
    private static final float AIM_X = 0.519f;
    private static final float AIM_Y = 0.432f;

    // Symbol offsets inside libGTASA.so (from its dynamic symbol table)
    private static final long OFF_MULT_Y = 0x8B07F8L; // CCamera::m_f3rdPersonCHairMultY
    private static final long OFF_MULT_X = 0x8B07FCL; // CCamera::m_f3rdPersonCHairMultX

    private static boolean started = false;

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

        long base = 0;
        String lastStatus = "";

        while (true) {
            String msg;
            try {
                if (base == 0) base = findLibBase("libGTASA.so");
                if (base == 0) {
                    msg = "libGTASA base not found";
                } else {
                    RandomAccessFile mem = new RandomAccessFile("/proc/self/mem", "rw");
                    try {
                        float curX = readFloat(mem, base + OFF_MULT_X);
                        float curY = readFloat(mem, base + OFF_MULT_Y);
                        // only touch memory once the game has initialised sane values
                        if (curX > 0.05f && curX < 0.95f && curY > 0.05f && curY < 0.95f) {
                            if (curX != AIM_X) writeFloat(mem, base + OFF_MULT_X, AIM_X);
                            if (curY != AIM_Y) writeFloat(mem, base + OFF_MULT_Y, AIM_Y);
                            msg = String.format(Locale.US, "ok x=%.4f y=%.4f", AIM_X, AIM_Y);
                        } else {
                            msg = String.format(Locale.US, "waiting: unexpected values x=%.4f y=%.4f", curX, curY);
                        }
                    } finally {
                        mem.close();
                    }
                }
            } catch (Throwable e) {
                msg = "error: " + e;
            }

            if (status != null && !msg.equals(lastStatus)) {
                try {
                    FileWriter w = new FileWriter(status);
                    w.write(msg + "\n");
                    w.close();
                } catch (Throwable ignored) {}
            }
            lastStatus = msg;

            try { Thread.sleep(300); } catch (InterruptedException e) { return; }
        }
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

    private static float readFloat(RandomAccessFile mem, long addr) throws Exception {
        byte[] b = new byte[4];
        mem.seek(addr);
        mem.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private static void writeFloat(RandomAccessFile mem, long addr, float v) throws Exception {
        byte[] b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v).array();
        mem.seek(addr);
        mem.write(b);
    }
}
