//----------------------------------------------------------------------------------
// File:            libs\src\com\nvidia\devtech\NvEventQueueActivity.java
// Samples Version: Android NVIDIA samples 2 
// Email:           tegradev@nvidia.com
// Forum:           http://developer.nvidia.com/tegra/forums/tegra-forums/android-development
//
// Copyright 2009-2010 NVIDIA Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//----------------------------------------------------------------------------------
package com.nvidia.devtech;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.touch.mobile.dark.R;
import com.touch.mobile.dark.gui.HudManager;
import com.touch.mobile.dark.gui.dialogs.Dialog;
import com.touch.mobile.dark.gui.Speedometer;
import com.touch.mobile.dark.gui.Notification;
import com.touch.mobile.dark.gui.Menu;
import com.touch.mobile.dark.gui.ChooseServer;
import com.touch.mobile.dark.gui.tab.Tab;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 A base class used to provide a native-code event-loop interface to an
 application.  This class is designed to be subclassed by the application
 with very little need to extend the Java.  Paired with its native static-link
 library, libnv_event.a, this package makes it possible for native applciations
 to avoid any direct use of Java code.  In addition, input and other events are
 automatically queued and provided to the application in native code via a
 classic event queue-like API.  EGL functionality such as bind/unbind and swap
 are also made available to the native code for ease of application porting.
 Please see the external SDK documentation for an introduction to the use of
 this class and its paired native library.
 */
public abstract class NvEventQueueActivity
        extends AppCompatActivity
        implements SensorEventListener, InputManager.InputListener, View.OnTouchListener, HeightProvider.HeightListener {

    private static NvEventQueueActivity instance = null;
    protected Handler handler = null;

    private int SwapBufferSkip = 0;

    protected boolean paused = false;

    protected boolean wantsMultitouch = false;

    protected boolean supportPauseResume = true;
    protected boolean ResumeEventDone = false;

    //accelerometer related
    protected boolean wantsAccelerometer = false;
    protected SensorManager mSensorManager = null;
    protected ClipboardManager mClipboardManager = null;
    protected int mSensorDelay = SensorManager.SENSOR_DELAY_GAME; //other options: SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_NORMAL and SensorManager.SENSOR_DELAY_UI
    protected Display display = null;

    FrameLayout mAndroidUI = null;

    private static final int EGL_RENDERABLE_TYPE = 0x3040;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_OPENGL_ES3_BIT = 64;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    EGL10 egl = null;
    GL11 gl = null;

    private boolean ranInit = false;
    protected EGLSurface eglSurface = null;
    protected EGLDisplay eglDisplay = null;
    protected EGLContext eglContext = null;
    protected EGLConfig eglConfig = null;

    protected SurfaceHolder cachedSurfaceHolder = null;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;

    private int fixedWidth = 0;
    private int fixedHeight = 0;
    private boolean HasGLExtensions = false;
    private String glVendor = null;
    private String glExtensions = null;
    private String glRenderer = null;
    private String glVersion = null;
    private boolean GameIsFocused = false;
    private boolean viewIsActive = false;

    private FrameLayout mRootFrame = null;
    private SurfaceView mSurfaceView = null;

    private InputManager mInputManager = null;
    private HeightProvider mHeightProvider = null;

    private Dialog mDialog = null;
    private HudManager mHudManager = null;
    private Speedometer mSpeedometer = null;
    private Notification mNotification = null;
    private Menu mMenu = null;
    private ChooseServer mChooseServer = null;
    private Tab mTab = null;

    /* *
     * Helper function to select fixed window size.
     * */
    public void setFixedSize(int fw, int fh)
    {
        fixedWidth = fw;
        fixedHeight = fh;
    }

    public native void onEventBackPressed();

    public native void onSettingsWindowSave();
    public native void onSettingsWindowDefaults(int category);

    public native void setNativeCutoutSettings(boolean b);
    public native void setNativeKeyboardSettings(boolean b);
    public native void setNativeFpsCounterSettings(boolean b);
    public native void setNativeOutfitGunsSettings(boolean b);
    public native void setNativeHpArmourText(boolean b);
    public native void setNativeRadarrect(boolean b);
    public native void setNativePcMoney(boolean b);
    public native void setNativeSkyBox(boolean b);
    public native void setNativeDialog(boolean b);
    public native void setNativeHud(boolean b);

    public native boolean getNativeCutoutSettings();
    public native boolean getNativeKeyboardSettings();
    public native boolean getNativeFpsCounterSettings();
    public native boolean getNativeOutfitGunsSettings();
    public native boolean getNativeHpArmourText();
    public native boolean getNativeRadarrect();
    public native boolean getNativePcMoney();
    public native boolean getNativeSkyBox();
    public native boolean getNativeDialog();
    public native boolean getNativeHud();

    public native void setNativeHudElementColor(int id, int a, int r, int g, int b);
    public native byte[] getNativeHudElementColor(int id);

    public native void setNativeHudElementPosition(int id, int x, int y);
    public native int[] getNativeHudElementPosition(int id);

    public native void setNativeHudElementScale(int id, int x, int y);
    public native int[] getNativeHudElementScale(int id);

    public native void setNativeWidgetPositionAndScale(int id, int x, int y, int scale);
    public native int[] getNativeWidgetPositionAndScale(int id);

    public native void sendCommand(byte[] str);

    // Типы
    // 1 - Меню
    // 2 - Сервер
    public native void sendRPC(int type, byte[] str, int action);

    public native int getLastServer();

    public String getHudElementColor(int id)
    {
        byte[] color = getNativeHudElementColor(id);
        String str = null;
        try {
            str = new String(color, "windows-1251");
        }
        catch(UnsupportedEncodingException e)
        {

        }

        return str;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEventBackPressed();
    }

    private int mUseFullscreen = 0;

    private void processCutout()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            if(mUseFullscreen == 1)
            {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
        }
    }

    public void setUseFullscreen(int b)
    {
        mUseFullscreen = b;
    }

    public void showClientSettings()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public static int dpToPx(float f, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, f, context.getResources().getDisplayMetrics());
    }

    public void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY



        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event)
    {
        if(view == mRootFrame)
        {
            if (wantsMultitouch)
            {
                // A second finger that lands on a HUD button never reaches the button while another
                // finger is already on the game (the root frame keeps intercepting), so hit-test it here.
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)
                {
                    int idx = event.getActionIndex();
                    clickHudButtonAt(event.getX(idx), event.getY(idx));
                }

                int x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0;
                // marshal up the data.
                int numEvents = event.getPointerCount();
                for (int i=0; i<numEvents; i++)
                {
                    // only use pointers 0 and 1, 2, 3
                    int pointerId = event.getPointerId(i);
                    if (pointerId == 0)
                    {
                        x1 = (int)event.getX(i);
                        y1 = (int)event.getY(i);
                    }
                    else if (pointerId == 1)
                    {
                        x2 = (int)event.getX(i);
                        y2 = (int)event.getY(i);
                    }
                    else if (pointerId == 2)
                    {
                        x3 = (int)event.getX(i);
                        y3 = (int)event.getY(i);
                    }
                }

                int pointerId = event.getPointerId(event.getActionIndex());
                int action = event.getActionMasked();
                customMultiTouchEvent(action, pointerId, x1, y1, x2, y2,
                        x3, y3);
            }
            else // old style input.*/
            {
                touchEvent(event.getAction(), (int)event.getX(), (int)event.getY(), event);
            }
        }
        return true;
    }

    private static final int[] HUD_CLICK_IDS = {
            R.id.hud_weapon, R.id.hud_menu, R.id.hud_inv, R.id.hud_donate
    };

    /** x,y are in mRootFrame coordinates. Clicks the HUD button under that point, if any. */
    private void clickHudButtonAt(float x, float y)
    {
        try
        {
            int[] rootLoc = new int[2];
            mRootFrame.getLocationOnScreen(rootLoc);
            float sx = rootLoc[0] + x;
            float sy = rootLoc[1] + y;
            float slop = 6f * getResources().getDisplayMetrics().density;
            int[] loc = new int[2];

            // pass 0: exact bounds, pass 1: with a small tolerance around the button
            for (int pass = 0; pass < 2; pass++)
            {
                float pad = pass == 0 ? 0f : slop;
                for (int id : HUD_CLICK_IDS)
                {
                    View v = findViewById(id);
                    if (v == null || !v.isShown() || !v.isEnabled()) continue;
                    v.getLocationOnScreen(loc);
                    if (sx >= loc[0] - pad && sx <= loc[0] + v.getWidth() + pad
                            && sy >= loc[1] - pad && sy <= loc[1] + v.getHeight() + pad)
                    {
                        v.performClick();
                        return;
                    }
                }
            }
        }
        catch (Throwable ignored) {}
    }

    private native void onNativeHeightChanged(int orientation, int height);

    @Override
    public void onHeightChanged(int orientation, int height)
    {
        if(mInputManager != null) {
            mInputManager.onHeightChanged(height);
        }

        Dialog dialog = mDialog;
        if (dialog != null) {
            dialog.onHeightChanged(height);
        }
    }

    public native void onWeaponChanged();

    public native void togglePlayer(int toggle);
    public native void showTab();

    public native void onTabClose();

    /**
     * Helper class used to pass raw data around.
     */
    public class RawData
    {
        /** The actual data bytes. */
        public byte[] data;
        /** The length of the data. */
        public int length;
    }
    /**
     * Helper class used to pass a raw texture around.
     */
    public class RawTexture extends RawData
    {
        /** The width of the texture. */
        public int width;
        /** The height of the texture. */
        public int height;
    }

    /**
     * Helper function to load a file into a {@link NvEventQueueActivity.RawData} object.
     * It'll first try loading the file from "/data/" and if the file doesn't
     * exist there, it'll try loading it from the assets directory inside the
     * .APK file. This is to allow the files inside the apk to be overridden
     * or not be part of the .APK at all during the development phase of the
     * application, decreasing the size needed to be transmitted to the device
     * between changes to the code.
     *
     * @param filename The file to load.
     * @return The RawData object representing the file's fully loaded data,
     * or null if loading failed.
     */
    public RawData loadFile(String filename)
    {
        InputStream is = null;
        RawData ret = new RawData();
        try {
            try
            {
                is = new FileInputStream("/data/" + filename);
            }
            catch (Exception e)
            {
                try
                {
                    is = getAssets().open(filename);
                }
                catch (Exception e2)
                {
                }
            }
            int size = is.available();
            ret.length = size;
            ret.data = new byte[size];
            is.read(ret.data);
        }
        catch (IOException ioe)
        {
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (Exception e) {}
            }
        }
        return ret;
    }

    /**
     * Helper function to load a texture file into a {@link NvEventQueueActivity.RawTexture} object.
     * It'll first try loading the texture from "/data/" and if the file doesn't
     * exist there, it'll try loading it from the assets directory inside the
     * .APK file. This is to allow the files inside the apk to be overridden
     * or not be part of the .APK at all during the development phase of the
     * application, decreasing the size needed to be transmitted to the device
     * between changes to the code.
     *
     * The texture data will be flipped and bit-twiddled to fit being loaded directly
     * into OpenGL ES via the glTexImage2D call.
     *
     * @param filename The file to load.
     * @return The RawTexture object representing the texture's fully loaded data,
     * or null if loading failed.
     */
    public RawTexture loadTexture(String filename)
    {
        RawTexture ret = new RawTexture();
        try {
            InputStream is = null;
            try
            {
                is = new FileInputStream("/data/" + filename);
            }
            catch (Exception e)
            {
                try
                {
                    is = getAssets().open(filename);
                }
                catch (Exception e2)
                {
                }
            }

            Bitmap bmp = BitmapFactory.decodeStream(is);
            ret.width = bmp.getWidth();
            ret.height = bmp.getHeight();
            int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

            // Flip texture
            int[] tmp = new int[bmp.getWidth()];
            final int w = bmp.getWidth();
            final int h = bmp.getHeight();
            for (int i = 0; i < h>>1; i++)
            {
                System.arraycopy(pixels, i*w, tmp, 0, w);
                System.arraycopy(pixels, (h-1-i)*w, pixels, i*w, w);
                System.arraycopy(tmp, 0, pixels, (h-1-i)*w, w);
            }

            // Convert from ARGB -> RGBA and put into the byte array
            ret.length = pixels.length * 4;
            ret.data = new byte[ret.length];
            int pos = 0;
            int bpos = 0;
            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++, pos++)
                {
                    int p = pixels[pos];
                    ret.data[bpos++] = (byte) ((p>>16)&0xff);
                    ret.data[bpos++] = (byte) ((p>> 8)&0xff);
                    ret.data[bpos++] = (byte) ((p>> 0)&0xff);
                    ret.data[bpos++] = (byte) ((p>>24)&0xff);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Function called when app requests accelerometer events.
     * Applications need/should NOT overide this function - it will provide
     * accelerometer events into the event queue that is accessible
     * via the calls in nv_event.h
     *
     * @param values0: values[0] passed to onSensorChanged(). For accelerometer: Acceleration minus Gx on the x-axis.
     * @param values1: values[1] passed to onSensorChanged(). For accelerometer: Acceleration minus Gy on the y-axis.
     * @param values2: values[2] passed to onSensorChanged(). For accelerometer: Acceleration minus Gz on the z-axis.
     * @return True if the event was handled.
     */
    public native boolean accelerometerEvent(float values0, float values1, float values2);

    /**
         
