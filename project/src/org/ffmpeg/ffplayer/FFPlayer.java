package org.ffmpeg.ffplayer;

import io.github.faywong.ffplayer.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ffmpeg.ffplayer.config.Settings;
import org.ffmpeg.ffplayer.render.AudioThread;
import org.ffmpeg.ffplayer.render.DefaultRender;
import org.ffmpeg.ffplayer.render.FFPlayerView;
import org.ffmpeg.ffplayer.util.Advertisement;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannedString;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;

public class FFPlayer extends Activity implements FFPlayerView.ScreenKeyboardHelper,
        FFPlayerView.AdHelper, FFPlayerView.AppHelper {
    private static final String LOG_TAG = "FFPlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load SDL Libraries & Settings first
        final Semaphore SDLLibsLoadingSem = new Semaphore(0);

        class Callback implements Runnable {
            FFPlayer p;

            Callback(FFPlayer _p) {
                p = _p;
            }

            public void run() {
/*                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }*/

                if (p.mAudioThread == null) {
                    System.out.println("libSDL: Loading libraries");
                    p.LoadLibraries();
                    System.out.println("libSDL: Loading ApplicationLibrary");
                    if (!Settings.CompatibilityHacksStaticInit) {
                        p.LoadApplicationLibrary(p);
                    }
                    p.mAudioThread = new AudioThread(p);
                    System.out.println("libSDL: Loading settings");
                    final Semaphore SettingsLoadingSem = new Semaphore(0);
                    class Callback2 implements Runnable {
                        public FFPlayer Parent;

                        public void run() {
                            Settings.load(Parent);
                            Log.d(LOG_TAG, "ready to release SettingsLoadingSem");
                            SettingsLoadingSem.release();
                            Log.d(LOG_TAG, "SettingsLoadingSem released");
                        }
                    }
                    Callback2 cb = new Callback2();
                    cb.Parent = p;
                    // p.runOnUiThread(cb);
                    (new Thread(cb)).start();
                    Log.d(LOG_TAG, "Ready to acquire SettingsLoadingSem");
                    SettingsLoadingSem.acquireUninterruptibly();
                    Log.d(LOG_TAG, "SettingsLoadingSem acquired");
                }
                initSDL();
                Log.d(LOG_TAG, "ready to release SDLLibsLoadingSem");
                SDLLibsLoadingSem.release();
                Log.d(LOG_TAG, "SDLLibsLoadingSem released");
            }
        }
        (new Thread(new Callback(this))).start();

        setRequestedOrientation(Settings.HorizontalOrientation ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        instance = this;
        // fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Settings.InhibitSuspend)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        _videoLayout = new FrameLayout(this);
        _ad = new Advertisement(this);
        if (_ad.getView() != null) {
            _videoLayout.addView(_ad.getView());
            _ad.getView().setLayoutParams(
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT));
        }
        SDLLibsLoadingSem.acquireUninterruptibly();
        Log.d(LOG_TAG, "SDLLibsLoadingSem acquired");
        Log.d(LOG_TAG, "ready to setContentView");

        setContentView(_videoLayout);
    }

    public void setUpStatusLabel() {
        FFPlayer Parent = this; // Too lazy to rename
        if (Parent._tv == null) {
            Parent._tv = new TextView(Parent);
            Parent._tv.setMaxLines(2);
            Parent._tv.setText(R.string.init);
            if (null != Parent._layout2) {
                Parent._layout2.addView(Parent._tv);
            }
        }
    }

    public void initSDL() {
        (new Thread(new Runnable() {
            public void run() {
                // int tries = 30;
                while (isCurrentOrientationHorizontal() != Settings.HorizontalOrientation) {
                    System.out
                            .println("libSDL: Waiting for screen orientation to change - the device is probably in the lockscreen mode");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    /*
                     * tries--; if( tries <= 0 ) { System.out.println(
                     * "libSDL: Giving up waiting for screen orientation change"
                     * ); break; }
                     */
                    if (_isPaused) {
                        System.out
                                .println("libSDL: Application paused, cancelling SDL initialization until it will be brought to foreground");
                        return;
                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        initSDLInternal();
                    }
                });
            }
        })).start();
    }

    private void initSDLInternal() {
        if (sdlInited)
            return;
        System.out.println("libSDL: Initializing video and SDL application");

        sdlInited = true;
        if (Settings.UseAccelerometerAsArrowKeys || Settings.AppUsesAccelerometer)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (_ad.getView() != null)
            _videoLayout.removeView(_ad.getView());
        _layout2 = null;
        _tv = null;
        _inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        _videoLayout = new FrameLayout(this);
        SetLayerType.get().setLayerType(_videoLayout);
        setContentView(_videoLayout);
        mVideoView = new FFPlayerView(this, this, this, this);
        mVideoView.setMediaController(new MediaController(this));
        SetLayerType.get().setLayerType(mVideoView);
        _videoLayout.addView(mVideoView);
        mVideoView.setFocusableInTouchMode(true);
        mVideoView.setFocusable(true);
        mVideoView.requestFocus();
        if (_ad.getView() != null) {
            _videoLayout.addView(_ad.getView());
            _ad.getView().setLayoutParams(
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));
        }
        // Receive keyboard events
        DimSystemStatusBar.get().dim(_videoLayout);
        DimSystemStatusBar.get().dim(mVideoView);

        Intent launchIntent = getIntent();
        Log.d(LOG_TAG, "onResume() launched by intent:" + launchIntent);
        if (null != launchIntent && launchIntent.getAction().equals(Intent.ACTION_VIEW)) {
            mVideoView.setMediaURI(launchIntent.getData());
            mVideoView.start();
        }
    }

    @Override
    protected void onPause() {
        _isPaused = true;
        if (mVideoView != null)
            mVideoView.onPause();
        // if( _ad.getView() != null )
        // _ad.getView().onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.onResume();
            DimSystemStatusBar.get().dim(_videoLayout);
            DimSystemStatusBar.get().dim(mVideoView);
        } else {
            initSDL();
        }
        // if( _ad.getView() != null )
        // _ad.getView().onResume();
        _isPaused = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        System.out.println("libSDL: onWindowFocusChanged: " + hasFocus
                + " - sending onPause/onResume");
        return;
        // if (hasFocus == false)
        // onPause();
        // else
        // onResume();
        /*
         * if (hasFocus == false) { synchronized(textInput) { // Send
         * 'SDLK_PAUSE' (to enter pause mode) to native code:
         * DefaultRenderer.nativeTextInput( 19, 19 ); } }
         */
    }

    public boolean isPaused() {
        return _isPaused;
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null)
            mVideoView.onDestroy();
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void showScreenKeyboardWithoutTextInputField() {
        _inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        _inputManager.showSoftInput(mVideoView, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void showScreenKeyboard(final String oldText, boolean sendBackspace) {
        if (Settings.CompatibilityHacksTextInputEmulatesHwKeyboard) {
            showScreenKeyboardWithoutTextInputField();
            return;
        }
        if (_screenKeyboard != null)
            return;
        class simpleKeyListener implements OnKeyListener {
            FFPlayer _parent;
            boolean  sendBackspace;

            simpleKeyListener(FFPlayer parent, boolean sendBackspace) {
                _parent = parent;
                this.sendBackspace = sendBackspace;
            };

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_UP)
                        && ((keyCode == KeyEvent.KEYCODE_ENTER) || (keyCode == KeyEvent.KEYCODE_BACK))) {
                    _parent.hideScreenKeyboard();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_CLEAR) {
                    if (sendBackspace && event.getAction() == KeyEvent.ACTION_UP) {
                        synchronized (textInput) {
                            DefaultRender.nativeTextInput(8, 0); // Send
                                                                 // backspace
                                                                 // to native
                                                                 // code
                        }
                    }
                    // EditText deletes two characters at a time, here's a hacky
                    // fix
                    if (event.getAction() == KeyEvent.ACTION_DOWN
                            && (event.getFlags() | KeyEvent.FLAG_SOFT_KEYBOARD) != 0) {
                        EditText t = (EditText) v;
                        int start = t.getSelectionStart(); // get cursor
                                                           // starting position
                        int end = t.getSelectionEnd(); // get cursor ending
                                                       // position
                        if (start < 0)
                            return true;
                        if (end < 0 || end == start) {
                            start--;
                            if (start < 0)
                                return true;
                            end = start + 1;
                        }
                        t.setText(t.getText().toString().substring(0, start)
                                + t.getText().toString().substring(end));
                        t.setSelection(start);
                        return true;
                    }
                }
                // System.out.println("Key " + keyCode + " flags " +
                // event.getFlags() + " action " + event.getAction());
                return false;
            }
        }
    };

    @Override
    public void hideScreenKeyboard() {
        if (_screenKeyboard == null)
            return;

        synchronized (textInput) {
            String text = _screenKeyboard.getText().toString();
            for (int i = 0; i < text.length(); i++) {
                DefaultRender.nativeTextInput((int) text.charAt(i), (int) text.codePointAt(i));
            }
        }
        DefaultRender.nativeTextInputFinished();
        mVideoView.setFocusableInTouchMode(true);
        mVideoView.setFocusable(true);
        mVideoView.requestFocus();
    };

    @Override
    public boolean isScreenKeyboardShown() {
        return _screenKeyboard != null;
    };

    @Override
    public void setScreenKeyboardHintMessage(String s) {
        _screenKeyboardHintMessage = s;
        // System.out.println("setScreenKeyboardHintMessage: " +
        // (_screenKeyboardHintMessage != null ? _screenKeyboardHintMessage :
        // getString(R.string.text_edit_click_here)));
        runOnUiThread(new Runnable() {
            public void run() {
                if (_screenKeyboard != null) {
                    String hint = _screenKeyboardHintMessage;
                    _screenKeyboard.setHint(hint != null ? hint
                            : getString(R.string.text_edit_click_here));
                }
            }
        });
    }

    final static int ADVERTISEMENT_POSITION_RIGHT  = -1;
    final static int ADVERTISEMENT_POSITION_BOTTOM = -1;
    final static int ADVERTISEMENT_POSITION_CENTER = -2;

    @Override
    public void setAdvertisementPosition(int x, int y) {

        if (_ad.getView() != null) {
            final FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.gravity = 0;
            layout.leftMargin = 0;
            layout.topMargin = 0;
            if (x == ADVERTISEMENT_POSITION_RIGHT)
                layout.gravity |= Gravity.RIGHT;
            else if (x == ADVERTISEMENT_POSITION_CENTER)
                layout.gravity |= Gravity.CENTER_HORIZONTAL;
            else {
                layout.gravity |= Gravity.LEFT;
                layout.leftMargin = x;
            }
            if (y == ADVERTISEMENT_POSITION_BOTTOM)
                layout.gravity |= Gravity.BOTTOM;
            else if (x == ADVERTISEMENT_POSITION_CENTER)
                layout.gravity |= Gravity.CENTER_VERTICAL;
            else {
                layout.gravity |= Gravity.TOP;
                layout.topMargin = y;
            }
            class Callback implements Runnable {
                public void run() {
                    _ad.getView().setLayoutParams(layout);
                }
            }
            ;
            runOnUiThread(new Callback());
        }
    }

    @Override
    public void setAdvertisementVisible(final int visible) {
        if (_ad.getView() != null) {
            class Callback implements Runnable {
                public void run() {
                    if (visible == 0)
                        _ad.getView().setVisibility(View.GONE);
                    else
                        _ad.getView().setVisibility(View.VISIBLE);
                }
            }
            runOnUiThread(new Callback());
        }
    }

    @Override
    public void getAdvertisementParams(int params[]) {
        for (int i = 0; i < 5; i++)
            params[i] = 0;
        if (_ad.getView() != null) {
            params[0] = (_ad.getView().getVisibility() == View.VISIBLE) ? 1 : 0;
            FrameLayout.LayoutParams layout = (FrameLayout.LayoutParams) _ad.getView()
                    .getLayoutParams();
            params[1] = layout.leftMargin;
            params[2] = layout.topMargin;
            params[3] = _ad.getView().getMeasuredWidth();
            params[4] = _ad.getView().getMeasuredHeight();
        }
    }

    @Override
    public void requestNewAdvertisement() {
        if (_ad.getView() != null) {
            class Callback implements Runnable {
                public void run() {
                    _ad.requestNewAd();
                }
            }
            runOnUiThread(new Callback());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event) {
        if (_screenKeyboard != null)
            _screenKeyboard.onKeyDown(keyCode, event);
        else if (mVideoView != null) {
            if (mVideoView.nativeKey(keyCode, 1) == 0)
                return super.onKeyDown(keyCode, event);
        } else if (keyListener != null) {
            keyListener.onKeyEvent(keyCode);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        if (_screenKeyboard != null)
            _screenKeyboard.onKeyUp(keyCode, event);
        else if (mVideoView != null) {
            if (mVideoView.nativeKey(keyCode, 0) == 0)
                return super.onKeyUp(keyCode, event);
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                DimSystemStatusBar.get().dim(_videoLayout);
                DimSystemStatusBar.get().dim(mVideoView);
            }
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        System.out.println("dispatchTouchEvent: " + ev.getAction() +
        " coords " + ev.getX() + ":" + ev.getY() );
        Log.d(LOG_TAG, "dispatchTouchEvent() _screenKeyboard:" + _screenKeyboard
                + " _ad.getView():" + _ad.getView() + " mVideoView:" + mVideoView
                + " touchListener:" + touchListener);
        if (_screenKeyboard != null)
            _screenKeyboard.dispatchTouchEvent(ev);
        else if (_ad.getView() != null
                && // User clicked the advertisement, ignore when user moved
                   // finger from game screen to advertisement or touches
                   // screen with several fingers
                ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN || (ev
                        .getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP)
                && _ad.getView().getLeft() <= (int) ev.getX()
                && _ad.getView().getRight() > (int) ev.getX()
                && _ad.getView().getTop() <= (int) ev.getY()
                && _ad.getView().getBottom() > (int) ev.getY())
            return super.dispatchTouchEvent(ev);
        else if (mVideoView != null)
            mVideoView.onTouchEvent(ev);
        else if (touchListener != null)
            touchListener.onTouchEvent(ev);
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        // System.out.println("dispatchGenericMotionEvent: " + ev.getAction() +
        // " coords " + ev.getX() + ":" + ev.getY() );
        // This code fails to run for Android 1.6, so there will be no generic
        // motion event for Andorid screen keyboard
        /*
         * if(_screenKeyboard != null)
         * _screenKeyboard.dispatchGenericMotionEvent(ev); else
         */
        if (mVideoView != null)
            mVideoView.onGenericMotionEvent(ev);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing here
    }

    public void setText(final String t) {
        class Callback implements Runnable {
            FFPlayer             Parent;
            public SpannedString text;

            public void run() {
                Parent.setUpStatusLabel();
                if (Parent._tv != null)
                    Parent._tv.setText(text);
            }
        }
        Callback cb = new Callback();
        cb.text = new SpannedString(t);
        cb.Parent = this;
        this.runOnUiThread(cb);
    }

    public void showTaskbarNotification() {
        showTaskbarNotification("SDL application paused", "SDL application",
                "Application is paused, click to activate");
    }

    // Stolen from SDL port by Mamaich
    public void showTaskbarNotification(String text0, String text1, String text2) {
        NotificationManager NotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, FFPlayer.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification n = new Notification(R.drawable.ffmpeg, text0, System.currentTimeMillis());
        n.setLatestEventInfo(this, text1, text2, pendingIntent);
        NotificationManager.notify(NOTIFY_ID, n);
    }

    public void hideTaskbarNotification() {
        NotificationManager NotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManager.cancel(NOTIFY_ID);
    }

    public void LoadLibraries() {
        try {
            if (Settings.NeedGles2)
                System.loadLibrary("GLESv2");
            System.out.println("libSDL: loaded GLESv2 lib");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("libSDL: Cannot load GLESv2 lib");
        }

        // ----- VCMI hack -----
        String[] binaryZipNames = { "binaries-" + android.os.Build.CPU_ABI + ".zip", "binaries.zip" };
        for (String binaryZip : binaryZipNames) {
            try {
                System.out.println("libSDL: Trying to extract binaries from assets " + binaryZip);

                InputStream in = null;
                try {
                    for (int i = 0;; i++) {
                        InputStream in2 = getAssets().open(binaryZip + String.format("%02d", i));
                        if (in == null)
                            in = in2;
                        else
                            in = new SequenceInputStream(in, in2);
                    }
                } catch (IOException ee) {
                    try {
                        if (in == null)
                            in = getAssets().open(binaryZip);
                    } catch (IOException eee) {
                    }
                }

                if (in == null)
                    throw new RuntimeException(
                            "libSDL: Extracting binaries failed, the .apk file packaged incorrectly");

                ZipInputStream zip = new ZipInputStream(in);

                File libDir = getFilesDir();
                try {
                    libDir.mkdirs();
                } catch (SecurityException ee) {
                }
                ;

                byte[] buf = new byte[16384];
                while (true) {
                    ZipEntry entry = null;
                    entry = zip.getNextEntry();
                    /*
                     * if( entry != null ) System.out.println("Extracting lib "
                     * + entry.getName());
                     */
                    if (entry == null) {
                        System.out.println("Extracting binaries finished");
                        break;
                    }
                    if (entry.isDirectory()) {
                        File outDir = new File(libDir.getAbsolutePath() + "/" + entry.getName());
                        if (!(outDir.exists() && outDir.isDirectory()))
                            outDir.mkdirs();
                        continue;
                    }

                    OutputStream out = null;
                    String path = libDir.getAbsolutePath() + "/" + entry.getName();
                    try {
                        File outDir = new File(path.substring(0, path.lastIndexOf("/")));
                        if (!(outDir.exists() && outDir.isDirectory()))
                            outDir.mkdirs();
                    } catch (SecurityException eeeeeee) {
                    }
                    ;

                    try {
                        CheckedInputStream check = new CheckedInputStream(
                                new FileInputStream(path), new CRC32());
                        while (check.read(buf, 0, buf.length) > 0) {
                        }
                        ;
                        check.close();
                        if (check.getChecksum().getValue() != entry.getCrc()) {
                            File ff = new File(path);
                            ff.delete();
                            throw new Exception();
                        }
                        System.out.println("File '" + path
                                + "' exists and passed CRC check - not overwriting it");
                        continue;
                    } catch (Exception eeeeee) {
                    }

                    System.out.println("Saving to file '" + path + "'");

                    out = new FileOutputStream(path);
                    int len = zip.read(buf);
                    while (len >= 0) {
                        if (len > 0)
                            out.write(buf, 0, len);
                        len = zip.read(buf);
                    }

                    out.flush();
                    out.close();
                    Settings.nativeChmod(path, 0755);
                }
            } catch (Exception eee) {
                // System.out.println("libSDL: Error: " + eee.toString());
            }
        }
        // ----- VCMI hack -----

        // Load all libraries
        try {
            for (String l : Settings.AppLibraries) {
                try {
                    String libname = System.mapLibraryName(l);
                    File libpath = new File(getFilesDir().getAbsolutePath() + "/../lib/" + libname);
                    System.out.println("libSDL: loading lib " + libpath.getAbsolutePath());
                    System.load(libpath.getPath());
                } catch (UnsatisfiedLinkError e) {
                    System.out.println("libSDL: error loading lib " + l + ": " + e.toString());
                    try {
                        String libname = System.mapLibraryName(l);
                        File libpath = new File(getFilesDir().getAbsolutePath() + "/" + libname);
                        System.out.println("libSDL: loading lib " + libpath.getAbsolutePath());
                        System.load(libpath.getPath());
                    } catch (UnsatisfiedLinkError ee) {
                        System.out.println("libSDL: error loading lib " + l + ": " + ee.toString());
                        System.loadLibrary(l);
                    }
                }
            }
        } catch (UnsatisfiedLinkError e) {
            try {
                System.out.println("libSDL: Extracting APP2SD-ed libs");

                InputStream in = null;
                try {
                    for (int i = 0;; i++) {
                        InputStream in2 = getAssets().open("bindata" + String.valueOf(i));
                        if (in == null)
                            in = in2;
                        else
                            in = new SequenceInputStream(in, in2);
                    }
                } catch (IOException ee) {
                }

                if (in == null)
                    throw new RuntimeException(
                            "libSDL: Extracting APP2SD-ed libs failed, the .apk file packaged incorrectly");

                ZipInputStream zip = new ZipInputStream(in);

                File libDir = getFilesDir();
                try {
                    libDir.mkdirs();
                } catch (SecurityException ee) {
                }
                ;

                byte[] buf = new byte[16384];
                while (true) {
                    ZipEntry entry = null;
                    entry = zip.getNextEntry();
                    /*
                     * if( entry != null ) System.out.println("Extracting lib "
                     * + entry.getName());
                     */
                    if (entry == null) {
                        System.out.println("Extracting libs finished");
                        break;
                    }
                    if (entry.isDirectory()) {
                        File outDir = new File(libDir.getAbsolutePath() + "/" + entry.getName());
                        if (!(outDir.exists() && outDir.isDirectory()))
                            outDir.mkdirs();
                        continue;
                    }

                    OutputStream out = null;
                    String path = libDir.getAbsolutePath() + "/" + entry.getName();
                    try {
                        File outDir = new File(path.substring(0, path.lastIndexOf("/")));
                        if (!(outDir.exists() && outDir.isDirectory()))
                            outDir.mkdirs();
                    } catch (SecurityException eeeee) {
                    }
                    ;

                    System.out.println("Saving to file '" + path + "'");

                    out = new FileOutputStream(path);
                    int len = zip.read(buf);
                    while (len >= 0) {
                        if (len > 0)
                            out.write(buf, 0, len);
                        len = zip.read(buf);
                    }

                    out.flush();
                    out.close();
                }

                for (String l : Settings.AppLibraries) {
                    String libname = System.mapLibraryName(l);
                    File libpath = new File(libDir, libname);
                    System.out.println("libSDL: loading lib " + libpath.getPath());
                    System.load(libpath.getPath());
                    libpath.delete();
                }
            } catch (Exception ee) {
                System.out.println("libSDL: Error: " + ee.toString());
            }
        }

    };

    public void LoadApplicationLibrary(final Context context) {
        Log.d(LOG_TAG, "LoadApplicationLibrary() in");
        String libs[] = { "application", "sdl_main" };
        try {
            for (String l : libs) {
                System.loadLibrary(l);
            }
        } catch (UnsatisfiedLinkError e) {
            System.out.println("libSDL: error loading lib: " + e.toString());
            try {
                for (String l : libs) {
                    String libname = System.mapLibraryName(l);
                    File libpath = new File(context.getFilesDir(), libname);
                    System.out.println("libSDL: loading lib " + libpath.getPath());
                    System.load(libpath.getPath());
                    libpath.delete();
                }
            } catch (UnsatisfiedLinkError ee) {
                System.out.println("libSDL: error loading lib: " + ee.toString());
            }
        }
    }

    public int getApplicationVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            System.out.println("libSDL: Cannot get the version of our own package: " + e);
        }
        return 0;
    }

    public boolean isCurrentOrientationHorizontal() {
        Display getOrient = getWindowManager().getDefaultDisplay();
        return getOrient.getWidth() >= getOrient.getHeight();
    }

    public FrameLayout getVideoLayout() {
        return _videoLayout;
    }

    static int                          NOTIFY_ID                  = 12367098;                 // Random
                                                                                                // ID

    private static FFPlayerView         mVideoView                 = null;
    private static AudioThread          mAudioThread               = null;

    private TextView                    _tv                        = null;
    private LinearLayout                _layout2                   = null;
    private Advertisement               _ad                        = null;

    private FrameLayout                 _videoLayout               = null;
    private EditText                    _screenKeyboard            = null;
    private String                      _screenKeyboardHintMessage = null;
    private boolean                     sdlInited                  = false;
    public Settings.TouchEventsListener touchListener              = null;
    public Settings.KeyEventsListener   keyListener                = null;
    boolean                             _isPaused                  = false;
    private InputMethodManager          _inputManager              = null;

    public LinkedList<Integer>          textInput                  = new LinkedList<Integer>();
    public static FFPlayer              instance                   = null;
}

// *** HONEYCOMB / ICS FIX FOR FULLSCREEN MODE, by lmak ***
abstract class DimSystemStatusBar {
    public static DimSystemStatusBar get() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
            return DimSystemStatusBarHoneycomb.Holder.sInstance;
        else
            return DimSystemStatusBarDummy.Holder.sInstance;
    }

    public abstract void dim(final View view);

    private static class DimSystemStatusBarHoneycomb extends DimSystemStatusBar {
        private static class Holder {
            private static final DimSystemStatusBarHoneycomb sInstance = new DimSystemStatusBarHoneycomb();
        }

        public void dim(final View view) {
            /*
             * if (android.os.Build.VERSION.SDK_INT >=
             * android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) { // ICS has
             * the same constant redefined with a different name.
             * hiddenStatusCode = android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;
             * }
             */
            view.setSystemUiVisibility(android.view.View.STATUS_BAR_HIDDEN);
        }
    }

    private static class DimSystemStatusBarDummy extends DimSystemStatusBar {
        private static class Holder {
            private static final DimSystemStatusBarDummy sInstance = new DimSystemStatusBarDummy();
        }

        public void dim(final View view) {
        }
    }
}

abstract class SetLayerType {
    public static SetLayerType get() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
            return SetLayerTypeHoneycomb.Holder.sInstance;
        else
            return SetLayerTypeDummy.Holder.sInstance;
    }

    public abstract void setLayerType(final View view);

    private static class SetLayerTypeHoneycomb extends SetLayerType {
        private static class Holder {
            private static final SetLayerTypeHoneycomb sInstance = new SetLayerTypeHoneycomb();
        }

        public void setLayerType(final View view) {
            view.setLayerType(android.view.View.LAYER_TYPE_NONE, null);
            // view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        }
    }

    private static class SetLayerTypeDummy extends SetLayerType {
        private static class Holder {
            private static final SetLayerTypeDummy sInstance = new SetLayerTypeDummy();
        }

        public void setLayerType(final View view) {
        }
    }
}
