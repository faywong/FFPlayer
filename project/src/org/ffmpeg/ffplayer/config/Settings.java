package org.ffmpeg.ffplayer.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.ffmpeg.ffplayer.FFPlayer;
import org.ffmpeg.ffplayer.render.SDLInput;
import org.ffmpeg.ffplayer.render.SDL_Keys;
import org.ffmpeg.ffplayer.render.SDLInput.Mouse;
import org.ffmpeg.ffplayer.util.AccelerometerReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.ffmpeg.ffplayer.R;

public class Settings {
    static String         SettingsFileName      = "libsdl-settings.cfg";

    static boolean        settingsLoaded        = false;
    public static boolean settingsChanged       = false;
    static final int      SETTINGS_FILE_VERSION = 5;
    public static String mediaUrl = null;

    static void Save(final FFPlayer p) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(p.openFileOutput(SettingsFileName,
                    p.MODE_WORLD_READABLE));
            out.writeInt(SETTINGS_FILE_VERSION);
            out.writeBoolean(PhoneHasArrowKeys);
            out.writeBoolean(PhoneHasTrackball);
            out.writeBoolean(UseAccelerometerAsArrowKeys);
            out.writeBoolean(UseTouchscreenKeyboard);
            out.writeInt(TouchscreenKeyboardSize);
            out.writeInt(AccelerometerSensitivity);
            out.writeInt(AccelerometerCenterPos);
            out.writeInt(TrackballDampening);
            out.writeInt(AudioBufferConfig);
            out.writeInt(TouchscreenKeyboardTheme);
            out.writeInt(RightClickMethod);
            out.writeInt(ShowScreenUnderFinger);
            out.writeInt(LeftClickMethod);
            out.writeBoolean(MoveMouseWithJoystick);
            out.writeBoolean(ClickMouseWithDpad);
            out.writeInt(ClickScreenPressure);
            out.writeInt(ClickScreenTouchspotSize);
            out.writeBoolean(KeepAspectRatio);
            out.writeInt(MoveMouseWithJoystickSpeed);
            out.writeInt(MoveMouseWithJoystickAccel);
            out.writeInt(SDL_Keys.JAVA_KEYCODE_LAST);
            for (int i = 0; i < SDL_Keys.JAVA_KEYCODE_LAST; i++) {
                out.writeInt(RemapHwKeycode[i]);
            }
            out.writeInt(RemapScreenKbKeycode.length);
            for (int i = 0; i < RemapScreenKbKeycode.length; i++) {
                out.writeInt(RemapScreenKbKeycode[i]);
            }
            out.writeInt(ScreenKbControlsShown.length);
            for (int i = 0; i < ScreenKbControlsShown.length; i++) {
                out.writeBoolean(ScreenKbControlsShown[i]);
            }
            out.writeInt(TouchscreenKeyboardTransparency);
            out.writeInt(RemapMultitouchGestureKeycode.length);
            for (int i = 0; i < RemapMultitouchGestureKeycode.length; i++) {
                out.writeInt(RemapMultitouchGestureKeycode[i]);
                out.writeBoolean(MultitouchGesturesUsed[i]);
            }
            out.writeInt(MultitouchGestureSensitivity);
            for (int i = 0; i < TouchscreenCalibration.length; i++)
                out.writeInt(TouchscreenCalibration[i]);
            out.writeInt(CommandLine.length());
            for (int i = 0; i < CommandLine.length(); i++)
                out.writeChar(CommandLine.charAt(i));
            out.writeInt(ScreenKbControlsLayout.length);
            for (int i = 0; i < ScreenKbControlsLayout.length; i++)
                for (int ii = 0; ii < 4; ii++)
                    out.writeInt(ScreenKbControlsLayout[i][ii]);
            out.writeInt(LeftClickKey);
            out.writeInt(RightClickKey);
            out.writeBoolean(VideoLinearFilter);
            out.writeInt(LeftClickTimeout);
            out.writeInt(RightClickTimeout);
            out.writeBoolean(RelativeMouseMovement);
            out.writeInt(RelativeMouseMovementSpeed);
            out.writeInt(RelativeMouseMovementAccel);
            out.writeBoolean(MultiThreadedVideo);
            out.writeBoolean(BrokenLibCMessageShown);
            out.writeInt(TouchscreenKeyboardDrawSize);
            out.writeInt(((FFPlayer) p).getApplicationVersion());
            out.writeFloat(gyro_x1);
            out.writeFloat(gyro_x2);
            out.writeFloat(gyro_xc);
            out.writeFloat(gyro_y1);
            out.writeFloat(gyro_y2);
            out.writeFloat(gyro_yc);
            out.writeFloat(gyro_z1);
            out.writeFloat(gyro_z2);
            out.writeFloat(gyro_zc);

            out.close();
            settingsLoaded = true;

        } catch (FileNotFoundException e) {
        } catch (SecurityException e) {
        } catch (IOException e) {
        }
        ;
    }

    public static void load(final FFPlayer p) {
        if (settingsLoaded) // Prevent starting twice
        {
            return;
        }
        System.out.println("libSDL: Settings.Load(): enter");
        nativeInitKeymap();
        
        for (int i = 0; i < SDL_Keys.JAVA_KEYCODE_LAST; i++) {
            int sdlKey = nativeGetKeymapKey(i);
            int idx = 0;
            for (int ii = 0; ii < SDL_Keys.values.length; ii++)
                if (SDL_Keys.values[ii] == sdlKey)
                    idx = ii;
            RemapHwKeycode[i] = idx;
        }
        for (int i = 0; i < RemapScreenKbKeycode.length; i++) {
            int sdlKey = nativeGetKeymapKeyScreenKb(i);
            int idx = 0;
            for (int ii = 0; ii < SDL_Keys.values.length; ii++)
                if (SDL_Keys.values[ii] == sdlKey)
                    idx = ii;
            RemapScreenKbKeycode[i] = idx;
        }
        ScreenKbControlsShown[0] = AppNeedsArrowKeys;
        ScreenKbControlsShown[1] = AppNeedsTextInput;
        for (int i = 2; i < ScreenKbControlsShown.length; i++)
            ScreenKbControlsShown[i] = (i - 2 < AppTouchscreenKeyboardKeysAmount);
        for (int i = 0; i < RemapMultitouchGestureKeycode.length; i++) {
            int sdlKey = nativeGetKeymapKeyMultitouchGesture(i);
            int idx = 0;
            for (int ii = 0; ii < SDL_Keys.values.length; ii++)
                if (SDL_Keys.values[ii] == sdlKey)
                    idx = ii;
            RemapMultitouchGestureKeycode[i] = idx;
        }
        for (int i = 0; i < MultitouchGesturesUsed.length; i++)
            MultitouchGesturesUsed[i] = true;

        System.out.println("android.os.Build.MODEL: " + android.os.Build.MODEL);
        if ((android.os.Build.MODEL.equals("GT-N7000") || android.os.Build.MODEL.equals("SGH-I717"))
                && android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Samsung Galaxy Note generates a keypress when you hover a
            // stylus over the screen, and that messes up OpenTTD dialogs
            // ICS update sends events in a proper way
            RemapHwKeycode[112] = SDLInput.SDL_1_2_Keycodes.SDLK_UNKNOWN;
        }

        try {
            ObjectInputStream settingsFile = new ObjectInputStream(new FileInputStream(p
                    .getFilesDir().getAbsolutePath() + "/" + SettingsFileName));
            if (settingsFile.readInt() != SETTINGS_FILE_VERSION)
                throw new IOException();
            PhoneHasArrowKeys = settingsFile.readBoolean();
            PhoneHasTrackball = settingsFile.readBoolean();
            UseAccelerometerAsArrowKeys = settingsFile.readBoolean();
            UseTouchscreenKeyboard = settingsFile.readBoolean();
            TouchscreenKeyboardSize = settingsFile.readInt();
            AccelerometerSensitivity = settingsFile.readInt();
            AccelerometerCenterPos = settingsFile.readInt();
            TrackballDampening = settingsFile.readInt();
            AudioBufferConfig = settingsFile.readInt();
            TouchscreenKeyboardTheme = settingsFile.readInt();
            RightClickMethod = settingsFile.readInt();
            ShowScreenUnderFinger = settingsFile.readInt();
            LeftClickMethod = settingsFile.readInt();
            MoveMouseWithJoystick = settingsFile.readBoolean();
            ClickMouseWithDpad = settingsFile.readBoolean();
            ClickScreenPressure = settingsFile.readInt();
            ClickScreenTouchspotSize = settingsFile.readInt();
            KeepAspectRatio = settingsFile.readBoolean();
            MoveMouseWithJoystickSpeed = settingsFile.readInt();
            MoveMouseWithJoystickAccel = settingsFile.readInt();
            int readKeys = settingsFile.readInt();
            for (int i = 0; i < readKeys; i++) {
                RemapHwKeycode[i] = settingsFile.readInt();
            }
            if (settingsFile.readInt() != RemapScreenKbKeycode.length)
                throw new IOException();
            for (int i = 0; i < RemapScreenKbKeycode.length; i++) {
                RemapScreenKbKeycode[i] = settingsFile.readInt();
            }
            if (settingsFile.readInt() != ScreenKbControlsShown.length)
                throw new IOException();
            for (int i = 0; i < ScreenKbControlsShown.length; i++) {
                ScreenKbControlsShown[i] = settingsFile.readBoolean();
            }
            TouchscreenKeyboardTransparency = settingsFile.readInt();
            if (settingsFile.readInt() != RemapMultitouchGestureKeycode.length)
                throw new IOException();
            for (int i = 0; i < RemapMultitouchGestureKeycode.length; i++) {
                RemapMultitouchGestureKeycode[i] = settingsFile.readInt();
                MultitouchGesturesUsed[i] = settingsFile.readBoolean();
            }
            MultitouchGestureSensitivity = settingsFile.readInt();
            for (int i = 0; i < TouchscreenCalibration.length; i++)
                TouchscreenCalibration[i] = settingsFile.readInt();

            StringBuilder b = new StringBuilder();
            int len = settingsFile.readInt();
            for (int i = 0; i < len; i++)
                b.append(settingsFile.readChar());
            CommandLine = b.toString();

            if (settingsFile.readInt() != ScreenKbControlsLayout.length)
                throw new IOException();
            for (int i = 0; i < ScreenKbControlsLayout.length; i++)
                for (int ii = 0; ii < 4; ii++)
                    ScreenKbControlsLayout[i][ii] = settingsFile.readInt();
            LeftClickKey = settingsFile.readInt();
            RightClickKey = settingsFile.readInt();
            VideoLinearFilter = settingsFile.readBoolean();
            LeftClickTimeout = settingsFile.readInt();
            RightClickTimeout = settingsFile.readInt();
            RelativeMouseMovement = settingsFile.readBoolean();
            RelativeMouseMovementSpeed = settingsFile.readInt();
            RelativeMouseMovementAccel = settingsFile.readInt();
            MultiThreadedVideo = settingsFile.readBoolean();

            BrokenLibCMessageShown = settingsFile.readBoolean();
            TouchscreenKeyboardDrawSize = settingsFile.readInt();
            int cfgVersion = settingsFile.readInt();
            gyro_x1 = settingsFile.readFloat();
            gyro_x2 = settingsFile.readFloat();
            gyro_xc = settingsFile.readFloat();
            gyro_y1 = settingsFile.readFloat();
            gyro_y2 = settingsFile.readFloat();
            gyro_yc = settingsFile.readFloat();
            gyro_z1 = settingsFile.readFloat();
            gyro_z2 = settingsFile.readFloat();
            gyro_zc = settingsFile.readFloat();

            settingsLoaded = true;

            System.out.println("libSDL: Settings.Load(): loaded settings successfully");
            settingsFile.close();

            System.out.println("libSDL: old cfg version " + cfgVersion + ", our version "
                    + p.getApplicationVersion());
            if (cfgVersion != ((FFPlayer) p).getApplicationVersion()) {
                DeleteFilesOnUpgrade();
                if (ResetSdlConfigForThisVersion) {
                    System.out.println("libSDL: old cfg version " + cfgVersion + ", our version "
                            + p.getApplicationVersion() + " and we need to clean up config file");
                    // Delete settings file, and restart the application
                    DeleteSdlConfigOnUpgradeAndRestart(p);
                }
                Save(p);
            }

            return;

        } catch (FileNotFoundException e) {
        } catch (SecurityException e) {
        } catch (IOException e) {
            DeleteFilesOnUpgrade();
            if (ResetSdlConfigForThisVersion) {
                System.out.println("libSDL: old cfg version unknown or too old, our version "
                        + p.getApplicationVersion() + " and we need to clean up config file");
                DeleteSdlConfigOnUpgradeAndRestart(p);
            }
        }

        System.out.println("libSDL: Settings.Load(): loading settings failed, running config dialog");
        ((FFPlayer) p).setUpStatusLabel();
        if (checkRamSize(p))
            showConfig(p, true);
    }

    // ===============================================================================================

    public static abstract class Menu {
        // Should be overridden by children
        abstract void run(final FFPlayer p);

        abstract String title(final FFPlayer p);

        boolean enabled() {
            return true;
        }

        // Should not be overridden
        boolean enabledOrHidden() {
            for (Settings.Menu m : HiddenMenuOptions) {
                if (m.getClass().getName().equals(this.getClass().getName()))
                    return false;
            }
            return enabled();
        }

        void showMenuOptionsList(final FFPlayer p, final Settings.Menu[] list) {
            menuStack.add(this);
            ArrayList<CharSequence> items = new ArrayList<CharSequence>();
            for (Settings.Menu m : list) {
                if (m.enabledOrHidden())
                    items.add(m.title(p));
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(title(p));
            builder.setItems(items.toArray(new CharSequence[0]),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            int selected = 0;

                            for (Settings.Menu m : list) {
                                if (m.enabledOrHidden()) {
                                    if (selected == item) {
                                        m.run(p);
                                        return;
                                    }
                                    selected++;
                                }
                            }
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBackOuterMenu(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static ArrayList<Settings.Menu> menuStack = new ArrayList<Settings.Menu>();

    public static float             gyro_x1;

    public static float             gyro_x2;

    public static float             gyro_xc;

    public static float             gyro_y1;

    public static float             gyro_y2;

    public static float             gyro_yc;

    public static float             gyro_z1;

    public static float             gyro_z2;

    public static float             gyro_zc;

    public static void showConfig(final FFPlayer p, final boolean firstStart) {
        settingsChanged = true;

        if (!firstStart)
            new MainMenu().run(p);
        else {
            if (StartupMenuButtonTimeout > 0) // If we did not
                                              // disable startup
                                              // menu altogether
            {
                for (Settings.Menu m : FirstStartMenuOptions) {
                    boolean hidden = false;
                    for (Settings.Menu m1 : HiddenMenuOptions) {
                        if (m1.getClass().getName().equals(m.getClass().getName()))
                            hidden = true;
                    }
                    if (!hidden)
                        menuStack.add(m);
                }
            }
            goBack(p);
        }
    }

    static void goBack(final FFPlayer p) {
        if (menuStack.isEmpty()) {
            Save(p);
        } else {
            Settings.Menu c = menuStack.remove(menuStack.size() - 1);
            c.run(p);
        }
    }

    static void goBackOuterMenu(final FFPlayer p) {
        if (!menuStack.isEmpty())
            menuStack.remove(menuStack.size() - 1);
        goBack(p);
    }

    static class OkButton extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.ok);
        }

        void run(final FFPlayer p) {
            goBackOuterMenu(p);
        }
    }

    public static class DummyMenu extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.ok);
        }

        void run(final FFPlayer p) {
            goBack(p);
        }
    }

    static class MainMenu extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.device_config);
        }

        void run(final FFPlayer p) {
            Settings.Menu options[] = { new KeyboardConfigMainMenu(), new MouseConfigMainMenu(),
                    new GyroscopeCalibration(), new AudioConfig(), new RemapHwKeysConfig(),
                    new ScreenGesturesConfig(), new VideoSettingsConfig(),
                    new ResetToDefaultsConfig(), new OkButton(), };
            showMenuOptionsList(p, options);
        }
    }

    static class MouseConfigMainMenu extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.mouse_emulation);
        }

        boolean enabled() {
            return AppUsesMouse;
        }

        void run(final FFPlayer p) {
            Settings.Menu options[] = { new DisplaySizeConfig(false), new LeftClickConfig(),
                    new RightClickConfig(), new AdditionalMouseConfig(), new JoystickMouseConfig(),
                    new TouchPressureMeasurementTool(), new CalibrateTouchscreenMenu(),
                    new OkButton(), };
            showMenuOptionsList(p, options);
        }
    }

    static class KeyboardConfigMainMenu extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.controls_screenkb);
        }

        boolean enabled() {
            return UseTouchscreenKeyboard;
        }

        void run(final FFPlayer p) {
            Settings.Menu options[] = { new ScreenKeyboardThemeConfig(),
                    new ScreenKeyboardSizeConfig(), new ScreenKeyboardDrawSizeConfig(),
                    new ScreenKeyboardTransparencyConfig(), new RemapScreenKbConfig(),
                    new CustomizeScreenKbLayout(), new OkButton(), };
            showMenuOptionsList(p, options);
        }
    }

    static class ScreenKeyboardSizeConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.controls_screenkb_size);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.controls_screenkb_large),
                    p.getResources().getString(R.string.controls_screenkb_medium),
                    p.getResources().getString(R.string.controls_screenkb_small),
                    p.getResources().getString(R.string.controls_screenkb_tiny) };

            for (int i = 0; i < ScreenKbControlsLayout.length; i++)
                for (int ii = 0; ii < 4; ii++)
                    ScreenKbControlsLayout[i][ii] = 0;

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.controls_screenkb_size));
            builder.setSingleChoiceItems(items, TouchscreenKeyboardSize,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            TouchscreenKeyboardSize = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class ScreenKeyboardDrawSizeConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.controls_screenkb_drawsize);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.controls_screenkb_large),
                    p.getResources().getString(R.string.controls_screenkb_medium),
                    p.getResources().getString(R.string.controls_screenkb_small),
                    p.getResources().getString(R.string.controls_screenkb_tiny) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.controls_screenkb_drawsize));
            builder.setSingleChoiceItems(items, TouchscreenKeyboardDrawSize,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            TouchscreenKeyboardDrawSize = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class ScreenKeyboardThemeConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.controls_screenkb_theme);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.controls_screenkb_by, "Ultimate Droid",
                            "Sean Stieber"),
                    p.getResources().getString(R.string.controls_screenkb_by, "Simple Theme",
                            "Beholder"),
                    p.getResources().getString(R.string.controls_screenkb_by, "Sun", "Sirea") };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.controls_screenkb_theme));
            builder.setSingleChoiceItems(items, TouchscreenKeyboardTheme,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            TouchscreenKeyboardTheme = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class ScreenKeyboardTransparencyConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.controls_screenkb_transparency);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.controls_screenkb_trans_0),
                    p.getResources().getString(R.string.controls_screenkb_trans_1),
                    p.getResources().getString(R.string.controls_screenkb_trans_2),
                    p.getResources().getString(R.string.controls_screenkb_trans_3),
                    p.getResources().getString(R.string.controls_screenkb_trans_4) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.controls_screenkb_transparency));
            builder.setSingleChoiceItems(items, TouchscreenKeyboardTransparency,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            TouchscreenKeyboardTransparency = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class AudioConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.audiobuf_question);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.audiobuf_verysmall),
                    p.getResources().getString(R.string.audiobuf_small),
                    p.getResources().getString(R.string.audiobuf_medium),
                    p.getResources().getString(R.string.audiobuf_large) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.audiobuf_question);
            builder.setSingleChoiceItems(items, AudioBufferConfig,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            AudioBufferConfig = item;
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    public static class DisplaySizeConfig extends Settings.Menu {
        boolean firstStart = false;

        public DisplaySizeConfig() {
            this.firstStart = false;
        }

        public DisplaySizeConfig(boolean firstStart) {
            this.firstStart = firstStart;
        }

        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.display_size_mouse);
        }

        void run(final FFPlayer p) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.display_size_tiny_touchpad),
                    p.getResources().getString(R.string.display_size_tiny),
                    p.getResources().getString(R.string.display_size_small),
                    p.getResources().getString(R.string.display_size_small_touchpad),
                    p.getResources().getString(R.string.display_size_large), };
            int _size_tiny_touchpad = 0;
            int _size_tiny = 1;
            int _size_small = 2;
            int _size_small_touchpad = 3;
            int _size_large = 4;
            int _more_options = 5;

            if (!SwVideoMode) {
                CharSequence[] items2 = {
                        p.getResources().getString(R.string.display_size_small_touchpad),
                        p.getResources().getString(R.string.display_size_large), };
                items = items2;
                _size_small_touchpad = 0;
                _size_large = 1;
                _size_tiny_touchpad = _size_tiny = _size_small = 1000;

            }
            if (firstStart) {
                CharSequence[] items2 = {
                        p.getResources().getString(R.string.display_size_tiny_touchpad),
                        p.getResources().getString(R.string.display_size_tiny),
                        p.getResources().getString(R.string.display_size_small),
                        p.getResources().getString(R.string.display_size_small_touchpad),
                        p.getResources().getString(R.string.display_size_large),
                        p.getResources().getString(R.string.show_more_options), };
                items = items2;
                if (!SwVideoMode) {
                    CharSequence[] items3 = {
                            p.getResources().getString(R.string.display_size_small_touchpad),
                            p.getResources().getString(R.string.display_size_large),
                            p.getResources().getString(R.string.show_more_options), };
                    items = items3;
                    _more_options = 3;
                }
            }
            // Java is so damn worse than C++11
            final int size_tiny_touchpad = _size_tiny_touchpad;
            final int size_tiny = _size_tiny;
            final int size_small = _size_small;
            final int size_small_touchpad = _size_small_touchpad;
            final int size_large = _size_large;
            final int more_options = _more_options;

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.display_size);
            class ClickListener implements DialogInterface.OnClickListener {
                public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    if (item == size_large) {
                        LeftClickMethod = Mouse.LEFT_CLICK_NORMAL;
                        RelativeMouseMovement = false;
                        ShowScreenUnderFinger = Mouse.ZOOM_NONE;
                    }
                    if (item == size_small) {
                        LeftClickMethod = Mouse.LEFT_CLICK_NEAR_CURSOR;
                        RelativeMouseMovement = false;
                        ShowScreenUnderFinger = Mouse.ZOOM_MAGNIFIER;
                    }
                    if (item == size_small_touchpad) {
                        LeftClickMethod = Mouse.LEFT_CLICK_WITH_TAP_OR_TIMEOUT;
                        RelativeMouseMovement = true;
                        ShowScreenUnderFinger = Mouse.ZOOM_NONE;
                    }
                    if (item == size_tiny) {
                        LeftClickMethod = Mouse.LEFT_CLICK_NEAR_CURSOR;
                        RelativeMouseMovement = false;
                        ShowScreenUnderFinger = Mouse.ZOOM_SCREEN_TRANSFORM;
                    }
                    if (item == size_tiny_touchpad) {
                        LeftClickMethod = Mouse.LEFT_CLICK_WITH_TAP_OR_TIMEOUT;
                        RelativeMouseMovement = true;
                        ShowScreenUnderFinger = Mouse.ZOOM_FULLSCREEN_MAGNIFIER;
                    }
                    if (item == more_options) {
                        menuStack.clear();
                        new MainMenu().run(p);
                        return;
                    }
                    goBack(p);
                }
            }
            builder.setItems(items, new ClickListener());
            /*
             * else builder.setSingleChoiceItems(items, ShowScreenUnderFinger ==
             * Mouse.ZOOM_NONE ? ( RelativeMouseMovement ? SwVideoMode ? 2 : 1 :
             * 0 ) : ( ShowScreenUnderFinger == Mouse.ZOOM_MAGNIFIER &&
             * SwVideoMode ) ? 1 : ShowScreenUnderFinger + 1, new
             * ClickListener());
             */
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class LeftClickConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.leftclick_question);
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.leftclick_normal),
                    p.getResources().getString(R.string.leftclick_near_cursor),
                    p.getResources().getString(R.string.leftclick_multitouch),
                    p.getResources().getString(R.string.leftclick_pressure),
                    p.getResources().getString(R.string.rightclick_key),
                    p.getResources().getString(R.string.leftclick_timeout),
                    p.getResources().getString(R.string.leftclick_tap),
                    p.getResources().getString(R.string.leftclick_tap_or_timeout) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.leftclick_question);
            builder.setSingleChoiceItems(items, LeftClickMethod,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            LeftClickMethod = item;
                            if (item == Mouse.LEFT_CLICK_WITH_KEY)
                                p.keyListener = new KeyRemapToolMouseClick(p, true);
                            else if (item == Mouse.LEFT_CLICK_WITH_TIMEOUT
                                    || item == Mouse.LEFT_CLICK_WITH_TAP_OR_TIMEOUT)
                                showLeftClickTimeoutConfig(p);
                            else
                                goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showLeftClickTimeoutConfig(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.leftclick_timeout_time_0),
                    p.getResources().getString(R.string.leftclick_timeout_time_1),
                    p.getResources().getString(R.string.leftclick_timeout_time_2),
                    p.getResources().getString(R.string.leftclick_timeout_time_3),
                    p.getResources().getString(R.string.leftclick_timeout_time_4) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.leftclick_timeout_time);
            builder.setSingleChoiceItems(items, LeftClickTimeout,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            LeftClickTimeout = item;
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class RightClickConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.rightclick_question);
        }

        boolean enabled() {
            return AppNeedsTwoButtonMouse;
        }

        void run(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.rightclick_none),
                    p.getResources().getString(R.string.rightclick_multitouch),
                    p.getResources().getString(R.string.rightclick_pressure),
                    p.getResources().getString(R.string.rightclick_key),
                    p.getResources().getString(R.string.leftclick_timeout) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.rightclick_question);
            builder.setSingleChoiceItems(items, RightClickMethod,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RightClickMethod = item;
                            dialog.dismiss();
                            if (item == Mouse.RIGHT_CLICK_WITH_KEY)
                                p.keyListener = new KeyRemapToolMouseClick(p, false);
                            else if (item == Mouse.RIGHT_CLICK_WITH_TIMEOUT)
                                showRightClickTimeoutConfig(p);
                            else
                                goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showRightClickTimeoutConfig(final FFPlayer p) {
            final CharSequence[] items = {
                    p.getResources().getString(R.string.leftclick_timeout_time_0),
                    p.getResources().getString(R.string.leftclick_timeout_time_1),
                    p.getResources().getString(R.string.leftclick_timeout_time_2),
                    p.getResources().getString(R.string.leftclick_timeout_time_3),
                    p.getResources().getString(R.string.leftclick_timeout_time_4) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.leftclick_timeout_time);
            builder.setSingleChoiceItems(items, RightClickTimeout,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RightClickTimeout = item;
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    public static class KeyRemapToolMouseClick implements KeyEventsListener {
        FFPlayer p;
        boolean  leftClick;

        public KeyRemapToolMouseClick(FFPlayer _p, boolean leftClick) {
            p = _p;
            p.setText(p.getResources().getString(R.string.remap_hwkeys_press));
            this.leftClick = leftClick;
        }

        public void onKeyEvent(final int keyCode) {
            p.keyListener = null;
            int keyIndex = keyCode;
            if (keyIndex < 0)
                keyIndex = 0;
            if (keyIndex > SDL_Keys.JAVA_KEYCODE_LAST)
                keyIndex = 0;

            if (leftClick)
                LeftClickKey = keyIndex;
            else
                RightClickKey = keyIndex;

            goBack(p);
        }
    }

    static class AdditionalMouseConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.pointandclick_question);
        }

        void run(final FFPlayer p) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.pointandclick_joystickmouse),
                    p.getResources().getString(R.string.click_with_dpadcenter),
                    p.getResources().getString(R.string.pointandclick_relative) };

            boolean defaults[] = { MoveMouseWithJoystick, ClickMouseWithDpad, RelativeMouseMovement };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.pointandclick_question));
            builder.setMultiChoiceItems(items, defaults,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                            if (item == 0)
                                MoveMouseWithJoystick = isChecked;
                            if (item == 1)
                                ClickMouseWithDpad = isChecked;
                            if (item == 2)
                                RelativeMouseMovement = isChecked;
                        }
                    });
            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            if (RelativeMouseMovement)
                                showRelativeMouseMovementConfig(p);
                            else
                                goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showRelativeMouseMovementConfig(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.accel_veryslow),
                    p.getResources().getString(R.string.accel_slow),
                    p.getResources().getString(R.string.accel_medium),
                    p.getResources().getString(R.string.accel_fast),
                    p.getResources().getString(R.string.accel_veryfast) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.pointandclick_relative_speed);
            builder.setSingleChoiceItems(items, RelativeMouseMovementSpeed,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RelativeMouseMovementSpeed = item;

                            dialog.dismiss();
                            showRelativeMouseMovementConfig1(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showRelativeMouseMovementConfig1(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.none),
                    p.getResources().getString(R.string.accel_slow),
                    p.getResources().getString(R.string.accel_medium),
                    p.getResources().getString(R.string.accel_fast) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.pointandclick_relative_accel);
            builder.setSingleChoiceItems(items, RelativeMouseMovementAccel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RelativeMouseMovementAccel = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class JoystickMouseConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.pointandclick_joystickmousespeed);
        }

        boolean enabled() {
            return MoveMouseWithJoystick;
        };

        void run(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.accel_slow),
                    p.getResources().getString(R.string.accel_medium),
                    p.getResources().getString(R.string.accel_fast) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.pointandclick_joystickmousespeed);
            builder.setSingleChoiceItems(items, MoveMouseWithJoystickSpeed,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            MoveMouseWithJoystickSpeed = item;

                            dialog.dismiss();
                            showJoystickMouseAccelConfig(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showJoystickMouseAccelConfig(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.none),
                    p.getResources().getString(R.string.accel_slow),
                    p.getResources().getString(R.string.accel_medium),
                    p.getResources().getString(R.string.accel_fast) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.pointandclick_joystickmouseaccel);
            builder.setSingleChoiceItems(items, MoveMouseWithJoystickAccel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            MoveMouseWithJoystickAccel = item;

                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    public interface TouchEventsListener {
        public void onTouchEvent(final MotionEvent ev);
    }

    public interface KeyEventsListener {
        public void onKeyEvent(final int keyCode);
    }

    static class TouchPressureMeasurementTool extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.measurepressure);
        }

        boolean enabled() {
            return RightClickMethod == Mouse.RIGHT_CLICK_WITH_PRESSURE
                    || LeftClickMethod == Mouse.LEFT_CLICK_WITH_PRESSURE;
        };

        void run(final FFPlayer p) {
            p.setText(p.getResources().getString(R.string.measurepressure_touchplease));
            p.touchListener = new TouchMeasurementTool(p);
        }

        public static class TouchMeasurementTool implements TouchEventsListener {
            FFPlayer           p;
            ArrayList<Integer> force          = new ArrayList<Integer>();
            ArrayList<Integer> radius         = new ArrayList<Integer>();
            static final int   maxEventAmount = 100;

            public TouchMeasurementTool(FFPlayer _p) {
                p = _p;
            }

            public void onTouchEvent(final MotionEvent ev) {
                force.add(new Integer((int) (ev.getPressure() * 1000.0)));
                radius.add(new Integer((int) (ev.getSize() * 1000.0)));
                p.setText(p.getResources().getString(R.string.measurepressure_response,
                        force.get(force.size() - 1), radius.get(radius.size() - 1)));
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                }

                if (force.size() >= maxEventAmount) {
                    p.touchListener = null;
                    ClickScreenPressure = getAverageForce();
                    ClickScreenTouchspotSize = getAverageRadius();
                    System.out.println("SDL: measured average force " + ClickScreenPressure
                            + " radius " + ClickScreenTouchspotSize);
                    goBack(p);
                }
            }

            int getAverageForce() {
                int avg = 0;
                for (Integer f : force) {
                    avg += f;
                }
                return avg / force.size();
            }

            int getAverageRadius() {
                int avg = 0;
                for (Integer r : radius) {
                    avg += r;
                }
                return avg / radius.size();
            }
        }
    }

    static class RemapHwKeysConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.remap_hwkeys);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            p.setText(p.getResources().getString(R.string.remap_hwkeys_press));
            p.keyListener = new KeyRemapTool(p);
        }

        public static class KeyRemapTool implements KeyEventsListener {
            FFPlayer p;

            public KeyRemapTool(FFPlayer _p) {
                p = _p;
            }

            public void onKeyEvent(final int keyCode) {
                p.keyListener = null;
                int keyIndex = keyCode;
                if (keyIndex < 0)
                    keyIndex = 0;
                if (keyIndex > SDL_Keys.JAVA_KEYCODE_LAST)
                    keyIndex = 0;

                final int KeyIndexFinal = keyIndex;
                AlertDialog.Builder builder = new AlertDialog.Builder(p);
                builder.setTitle(R.string.remap_hwkeys_select);
                builder.setSingleChoiceItems(SDL_Keys.namesSorted,
                        SDL_Keys.namesSortedBackIdx[RemapHwKeycode[keyIndex]],
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                RemapHwKeycode[KeyIndexFinal] = SDL_Keys.namesSortedIdx[item];

                                dialog.dismiss();
                                goBack(p);
                            }
                        });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        goBack(p);
                    }
                });
                AlertDialog alert = builder.create();
                alert.setOwnerActivity(p);
                alert.show();
            }
        }
    }

    static class RemapScreenKbConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.remap_screenkb);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            CharSequence[] items = { p.getResources().getString(R.string.remap_screenkb_joystick),
                    p.getResources().getString(R.string.remap_screenkb_button_text),
                    p.getResources().getString(R.string.remap_screenkb_button) + " 1",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 2",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 3",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 4",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 5",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 6", };

            boolean defaults[] = { ScreenKbControlsShown[0], ScreenKbControlsShown[1],
                    ScreenKbControlsShown[2], ScreenKbControlsShown[3], ScreenKbControlsShown[4],
                    ScreenKbControlsShown[5], ScreenKbControlsShown[6], ScreenKbControlsShown[7], };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.remap_screenkb));
            builder.setMultiChoiceItems(items, defaults,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                            if (!UseTouchscreenKeyboard)
                                item += 8;
                            ScreenKbControlsShown[item] = isChecked;
                        }
                    });
            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            showRemapScreenKbConfig2(p, 0);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showRemapScreenKbConfig2(final FFPlayer p, final int currentButton) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.remap_screenkb_button) + " 1",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 2",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 3",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 4",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 5",
                    p.getResources().getString(R.string.remap_screenkb_button) + " 6", };

            if (currentButton >= RemapScreenKbKeycode.length) {
                goBack(p);
                return;
            }
            if (!ScreenKbControlsShown[currentButton + 2]) {
                showRemapScreenKbConfig2(p, currentButton + 1);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(items[currentButton]);
            builder.setSingleChoiceItems(SDL_Keys.namesSorted,
                    SDL_Keys.namesSortedBackIdx[RemapScreenKbKeycode[currentButton]],
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RemapScreenKbKeycode[currentButton] = SDL_Keys.namesSortedIdx[item];

                            dialog.dismiss();
                            showRemapScreenKbConfig2(p, currentButton + 1);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class ScreenGesturesConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.remap_screenkb_button_gestures);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.remap_screenkb_button_zoomin),
                    p.getResources().getString(R.string.remap_screenkb_button_zoomout),
                    p.getResources().getString(R.string.remap_screenkb_button_rotateleft),
                    p.getResources().getString(R.string.remap_screenkb_button_rotateright), };

            boolean defaults[] = { MultitouchGesturesUsed[0], MultitouchGesturesUsed[1],
                    MultitouchGesturesUsed[2], MultitouchGesturesUsed[3], };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.remap_screenkb_button_gestures));
            builder.setMultiChoiceItems(items, defaults,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                            MultitouchGesturesUsed[item] = isChecked;
                        }
                    });
            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            showScreenGesturesConfig2(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showScreenGesturesConfig2(final FFPlayer p) {
            final CharSequence[] items = { p.getResources().getString(R.string.accel_slow),
                    p.getResources().getString(R.string.accel_medium),
                    p.getResources().getString(R.string.accel_fast),
                    p.getResources().getString(R.string.accel_veryfast) };

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(R.string.remap_screenkb_button_gestures_sensitivity);
            builder.setSingleChoiceItems(items, MultitouchGestureSensitivity,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            MultitouchGestureSensitivity = item;

                            dialog.dismiss();
                            showScreenGesturesConfig3(p, 0);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        static void showScreenGesturesConfig3(final FFPlayer p, final int currentButton) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.remap_screenkb_button_zoomin),
                    p.getResources().getString(R.string.remap_screenkb_button_zoomout),
                    p.getResources().getString(R.string.remap_screenkb_button_rotateleft),
                    p.getResources().getString(R.string.remap_screenkb_button_rotateright), };

            if (currentButton >= RemapMultitouchGestureKeycode.length) {
                goBack(p);
                return;
            }
            if (!MultitouchGesturesUsed[currentButton]) {
                showScreenGesturesConfig3(p, currentButton + 1);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(items[currentButton]);
            builder.setSingleChoiceItems(SDL_Keys.namesSorted,
                    SDL_Keys.namesSortedBackIdx[RemapMultitouchGestureKeycode[currentButton]],
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            RemapMultitouchGestureKeycode[currentButton] = SDL_Keys.namesSortedIdx[item];

                            dialog.dismiss();
                            showScreenGesturesConfig3(p, currentButton + 1);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class CalibrateTouchscreenMenu extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.calibrate_touchscreen);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            p.setText(p.getResources().getString(R.string.calibrate_touchscreen_touch));
            TouchscreenCalibration[0] = 0;
            TouchscreenCalibration[1] = 0;
            TouchscreenCalibration[2] = 0;
            TouchscreenCalibration[3] = 0;
            CalibrateTouchscreenMenu.ScreenEdgesCalibrationTool tool = new ScreenEdgesCalibrationTool(
                    p);
            p.touchListener = tool;
            p.keyListener = tool;
        }

        static class ScreenEdgesCalibrationTool implements TouchEventsListener, KeyEventsListener {
            FFPlayer  p;
            ImageView img;
            Bitmap    bmp;

            public ScreenEdgesCalibrationTool(FFPlayer _p) {
            }

            public void onTouchEvent(final MotionEvent ev) {
                if (TouchscreenCalibration[0] == TouchscreenCalibration[1]
                        && TouchscreenCalibration[1] == TouchscreenCalibration[2]
                        && TouchscreenCalibration[2] == TouchscreenCalibration[3]) {
                    TouchscreenCalibration[0] = (int) ev.getX();
                    TouchscreenCalibration[1] = (int) ev.getY();
                    TouchscreenCalibration[2] = (int) ev.getX();
                    TouchscreenCalibration[3] = (int) ev.getY();
                }
                if (ev.getX() < TouchscreenCalibration[0])
                    TouchscreenCalibration[0] = (int) ev.getX();
                if (ev.getY() < TouchscreenCalibration[1])
                    TouchscreenCalibration[1] = (int) ev.getY();
                if (ev.getX() > TouchscreenCalibration[2])
                    TouchscreenCalibration[2] = (int) ev.getX();
                if (ev.getY() > TouchscreenCalibration[3])
                    TouchscreenCalibration[3] = (int) ev.getY();
                Matrix m = new Matrix();
                RectF src = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
                RectF dst = new RectF(TouchscreenCalibration[0], TouchscreenCalibration[1],
                        TouchscreenCalibration[2], TouchscreenCalibration[3]);
                m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                img.setImageMatrix(m);
            }

            public void onKeyEvent(final int keyCode) {
                p.touchListener = null;
                p.keyListener = null;
                p.getVideoLayout().removeView(img);
                goBack(p);
            }
        }
    }

    static class CustomizeScreenKbLayout extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.screenkb_custom_layout);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            p.setText(p.getResources().getString(R.string.screenkb_custom_layout_help));
            CustomizeScreenKbLayout.CustomizeScreenKbLayoutTool tool = new CustomizeScreenKbLayoutTool(
                    p);
            p.touchListener = tool;
            p.keyListener = tool;
        }

        static class CustomizeScreenKbLayoutTool implements TouchEventsListener, KeyEventsListener {
            FFPlayer    p;
            FrameLayout layout        = null;
            ImageView   imgs[]        = new ImageView[ScreenKbControlsLayout.length];
            Bitmap      bmps[]        = new Bitmap[ScreenKbControlsLayout.length];
            ImageView   boundary      = null;
            Bitmap      boundaryBmp   = null;
            int         currentButton = 0;
            int         buttons[]     = { /*R.drawable.dpad, R.drawable.keyboard, R.drawable.b1,
                                              R.drawable.b2, R.drawable.b3, R.drawable.b4,
                                              R.drawable.b5, R.drawable.b6*/ };

            public CustomizeScreenKbLayoutTool(FFPlayer _p) {
            }

            void setupButton(boolean undo) {
                do {
                    currentButton += (undo ? -1 : 1);
                    if (currentButton >= ScreenKbControlsLayout.length) {
                        p.getVideoLayout().removeView(layout);
                        layout = null;
                        p.touchListener = null;
                        p.keyListener = null;
                        goBack(p);
                        return;
                    }
                    if (currentButton < 0) {
                        currentButton = 0;
                        undo = false;
                    }
                } while (!ScreenKbControlsShown[currentButton]);

                if (ScreenKbControlsLayout[currentButton][0] == ScreenKbControlsLayout[currentButton][2]
                        || ScreenKbControlsLayout[currentButton][1] == ScreenKbControlsLayout[currentButton][3]) {
                    int displayX = 800;
                    int displayY = 480;
                    try {
                        DisplayMetrics dm = new DisplayMetrics();
                        p.getWindowManager().getDefaultDisplay().getMetrics(dm);
                        displayX = dm.widthPixels;
                        displayY = dm.heightPixels;
                    } catch (Exception eeeee) {
                    }
                    ScreenKbControlsLayout[currentButton][0] = displayX / 2 - displayX / 6;
                    ScreenKbControlsLayout[currentButton][2] = displayX / 2 + displayX / 6;
                    ScreenKbControlsLayout[currentButton][1] = displayY / 2 - displayY / 4;
                    ScreenKbControlsLayout[currentButton][3] = displayY / 2 + displayY / 4;
                }
                Matrix m = new Matrix();
                RectF src = new RectF(0, 0, bmps[currentButton].getWidth(),
                        bmps[currentButton].getHeight());
                RectF dst = new RectF(ScreenKbControlsLayout[currentButton][0],
                        ScreenKbControlsLayout[currentButton][1],
                        ScreenKbControlsLayout[currentButton][2],
                        ScreenKbControlsLayout[currentButton][3]);
                m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                imgs[currentButton].setImageMatrix(m);
                m = new Matrix();
                src = new RectF(0, 0, boundaryBmp.getWidth(), boundaryBmp.getHeight());
                m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                boundary.setImageMatrix(m);
                String buttonText = (currentButton == 0 ? "DPAD"
                        : (currentButton == 1 ? "Text input" : ""));
                if (currentButton >= 2
                        && currentButton - 2 < AppTouchscreenKeyboardKeysNames.length)
                    buttonText = AppTouchscreenKeyboardKeysNames[currentButton - 2];
                p.setText(p.getResources().getString(R.string.screenkb_custom_layout_help) + "\n"
                        + buttonText.replace("_", " "));
            }

            public void onTouchEvent(final MotionEvent ev) {
                if (currentButton >= ScreenKbControlsLayout.length) {
                    setupButton(false);
                    return;
                }
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    ScreenKbControlsLayout[currentButton][0] = (int) ev.getX();
                    ScreenKbControlsLayout[currentButton][1] = (int) ev.getY();
                    ScreenKbControlsLayout[currentButton][2] = (int) ev.getX();
                    ScreenKbControlsLayout[currentButton][3] = (int) ev.getY();
                }
                if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                    if (ScreenKbControlsLayout[currentButton][0] > (int) ev.getX())
                        ScreenKbControlsLayout[currentButton][0] = (int) ev.getX();
                    if (ScreenKbControlsLayout[currentButton][1] > (int) ev.getY())
                        ScreenKbControlsLayout[currentButton][1] = (int) ev.getY();
                    if (ScreenKbControlsLayout[currentButton][2] < (int) ev.getX())
                        ScreenKbControlsLayout[currentButton][2] = (int) ev.getX();
                    if (ScreenKbControlsLayout[currentButton][3] < (int) ev.getY())
                        ScreenKbControlsLayout[currentButton][3] = (int) ev.getY();
                }

                Matrix m = new Matrix();
                RectF src = new RectF(0, 0, bmps[currentButton].getWidth(),
                        bmps[currentButton].getHeight());
                RectF dst = new RectF(ScreenKbControlsLayout[currentButton][0],
                        ScreenKbControlsLayout[currentButton][1],
                        ScreenKbControlsLayout[currentButton][2],
                        ScreenKbControlsLayout[currentButton][3]);
                m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                imgs[currentButton].setImageMatrix(m);
                m = new Matrix();
                src = new RectF(0, 0, boundaryBmp.getWidth(), boundaryBmp.getHeight());
                m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                boundary.setImageMatrix(m);

                if (ev.getAction() == MotionEvent.ACTION_UP)
                    setupButton(false);
            }

            public void onKeyEvent(final int keyCode) {
                if (layout != null && imgs[currentButton] != null)
                    layout.removeView(imgs[currentButton]);
                imgs[currentButton] = null;
                setupButton(true);
            }
        }
    }

    static class VideoSettingsConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.video);
        }

        // boolean enabled() { return true; };
        void run(final FFPlayer p) {
            CharSequence[] items = {
                    p.getResources().getString(R.string.pointandclick_keepaspectratio),
                    p.getResources().getString(R.string.video_smooth) };
            boolean defaults[] = { KeepAspectRatio, VideoLinearFilter };

            if (SwVideoMode && !CompatibilityHacksVideo) {
                CharSequence[] items2 = {
                        p.getResources().getString(R.string.pointandclick_keepaspectratio),
                        p.getResources().getString(R.string.video_smooth),
                        p.getResources().getString(R.string.video_separatethread), };
                boolean defaults2[] = { KeepAspectRatio, VideoLinearFilter, MultiThreadedVideo };
                items = items2;
                defaults = defaults2;
            }

            if (Using_SDL_1_3) {
                CharSequence[] items2 = { p.getResources().getString(
                        R.string.pointandclick_keepaspectratio), };
                boolean defaults2[] = { KeepAspectRatio, };
                items = items2;
                defaults = defaults2;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.video));
            builder.setMultiChoiceItems(items, defaults,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                            if (item == 0)
                                KeepAspectRatio = isChecked;
                            if (item == 1)
                                VideoLinearFilter = isChecked;
                            if (item == 2)
                                MultiThreadedVideo = isChecked;
                        }
                    });
            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    static class ShowReadme extends Settings.Menu {
        String title(final FFPlayer p) {
            return "Readme";
        }

        boolean enabled() {
            return true;
        }

        void run(final FFPlayer p) {
            String readmes[] = ReadmeText.split("\\^");
            String lang = new String(Locale.getDefault().getLanguage()) + ":";
            String readme = readmes[0];
            for (String r : readmes) {
                if (r.startsWith(lang))
                    readme = r.substring(lang.length());
            }
            TextView text = new TextView(p);
            text.setMaxLines(1000);
            text.setText(readme);
            text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.FILL_PARENT));
            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            ScrollView scroll = new ScrollView(p);
            scroll.addView(text);
            Button ok = new Button(p);
            final AlertDialog alertDismiss[] = new AlertDialog[1];
            ok.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    alertDismiss[0].cancel();
                }
            });
            ok.setText(R.string.ok);
            LinearLayout layout = new LinearLayout(p);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(scroll);
            layout.addView(ok);
            builder.setView(layout);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alertDismiss[0] = alert;
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    public static class GyroscopeCalibration extends Settings.Menu implements SensorEventListener {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.calibrate_gyroscope);
        }

        boolean enabled() {
            return AppUsesGyroscope;
        }

        void run(final FFPlayer p) {
            if (!AppUsesGyroscope || !AccelerometerReader.gyro.available(p)) {
                Toast toast = Toast.makeText(p,
                        p.getResources().getString(R.string.calibrate_gyroscope_not_supported),
                        Toast.LENGTH_LONG);
                toast.show();
                goBack(p);
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.calibrate_gyroscope));
            builder.setMessage(p.getResources().getString(R.string.calibrate_gyroscope_text));
            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            startCalibration(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }

        ImageView img;
        Bitmap    bmp;
        int       numEvents;
        FFPlayer  p;

        void startCalibration(final FFPlayer _p) {
        }

        public void onSensorChanged(SensorEvent event) {
            gyroscopeEvent(event.values[0], event.values[1], event.values[2]);
        }

        public void onAccuracyChanged(Sensor s, int a) {
        }

        void gyroscopeEvent(float x, float y, float z) {
            numEvents++;
            AccelerometerReader.gyro.xc += x;
            AccelerometerReader.gyro.yc += y;
            AccelerometerReader.gyro.zc += z;
            AccelerometerReader.gyro.x1 = Math.min(AccelerometerReader.gyro.x1, x * 1.1f); // Small
                                                                                           // safety
                                                                                           // bound
                                                                                           // coefficient
            AccelerometerReader.gyro.x2 = Math.max(AccelerometerReader.gyro.x2, x * 1.1f);
            AccelerometerReader.gyro.y1 = Math.min(AccelerometerReader.gyro.y1, y * 1.1f);
            AccelerometerReader.gyro.y2 = Math.max(AccelerometerReader.gyro.y2, y * 1.1f);
            AccelerometerReader.gyro.z1 = Math.min(AccelerometerReader.gyro.z1, z * 1.1f);
            AccelerometerReader.gyro.z2 = Math.max(AccelerometerReader.gyro.z2, z * 1.1f);
            final Matrix m = new Matrix();
            RectF src = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
            RectF dst = new RectF(x * 5000 + p.getVideoLayout().getWidth() / 2 - 50, y * 5000
                    + p.getVideoLayout().getHeight() / 2 - 50, x * 5000
                    + p.getVideoLayout().getWidth() / 2 + 50, y * 5000
                    + p.getVideoLayout().getHeight() / 2 + 50);
            m.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
            p.runOnUiThread(new Runnable() {
                public void run() {
                    img.setImageMatrix(m);
                }
            });
        }

        void finishCalibration(final FFPlayer p) {
            AccelerometerReader.gyro.unregisterListener(p, this);
            try {
                Thread.sleep(200); // Just in case we have pending events
            } catch (Exception e) {
            }
            if (numEvents > 5) {
                AccelerometerReader.gyro.xc /= (float) numEvents;
                AccelerometerReader.gyro.yc /= (float) numEvents;
                AccelerometerReader.gyro.zc /= (float) numEvents;
            }
            p.runOnUiThread(new Runnable() {
                public void run() {
                    p.getVideoLayout().removeView(img);
                    goBack(p);
                }
            });
        }
    }

    static class ResetToDefaultsConfig extends Settings.Menu {
        String title(final FFPlayer p) {
            return p.getResources().getString(R.string.reset_config);
        }

        boolean enabled() {
            return true;
        }

        void run(final FFPlayer p) {
            AlertDialog.Builder builder = new AlertDialog.Builder(p);
            builder.setTitle(p.getResources().getString(R.string.reset_config_ask));
            builder.setMessage(p.getResources().getString(R.string.reset_config_ask));

            builder.setPositiveButton(p.getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            DeleteSdlConfigOnUpgradeAndRestart(p); // Never
                                                                   // returns
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setNegativeButton(p.getResources().getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                            goBack(p);
                        }
                    });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    goBack(p);
                }
            });
            AlertDialog alert = builder.create();
            alert.setOwnerActivity(p);
            alert.show();
        }
    }

    // ===============================================================================================

    public static boolean deleteRecursively(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteRecursively(new File(dir, children[i]));
                if (!success)
                    return false;
            }
        }
        return dir.delete();
    }

    public static void DeleteFilesOnUpgrade() {
        String[] files = DeleteFilesOnUpgrade.split(" ");
        for (String path : files) {
            if (path.equals(""))
                continue;
            File f = new File(path);
            if (!f.exists())
                continue;
            deleteRecursively(f);
        }
    }

    public static void DeleteSdlConfigOnUpgradeAndRestart(final FFPlayer p) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(p.openFileOutput(SettingsFileName,
                    p.MODE_WORLD_READABLE));
            out.writeInt(-1);
            out.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        new File(p.getFilesDir() + "/" + SettingsFileName).delete();
        PendingIntent intent = PendingIntent.getActivity(p, 0, new Intent(p.getIntent()), p
                .getIntent().getFlags());
        AlarmManager mgr = (AlarmManager) p.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, intent);
        System.exit(0);
    }

    // ===============================================================================================

    public static void Apply(Context mContext) {
        nativeSetVideoDepth(VideoDepthBpp, NeedGles2 ? 1 : 0);
        if (VideoLinearFilter)
            nativeSetVideoLinearFilter();
        if (CompatibilityHacksVideo) {
            MultiThreadedVideo = true;
            SwVideoMode = true;
            nativeSetCompatibilityHacks();
        }
        if (SwVideoMode)
            nativeSetVideoForceSoftwareMode();
        if (SwVideoMode && MultiThreadedVideo)
            nativeSetVideoMultithreaded();
        if (PhoneHasTrackball)
            nativeSetTrackballUsed();
        if (AppUsesMouse)
            nativeSetMouseUsed(RightClickMethod, ShowScreenUnderFinger, LeftClickMethod,
                    MoveMouseWithJoystick ? 1 : 0, ClickMouseWithDpad ? 1 : 0, ClickScreenPressure,
                    ClickScreenTouchspotSize, MoveMouseWithJoystickSpeed,
                    MoveMouseWithJoystickAccel, LeftClickKey, RightClickKey, LeftClickTimeout,
                    RightClickTimeout, RelativeMouseMovement ? 1 : 0, RelativeMouseMovementSpeed,
                    RelativeMouseMovementAccel, ShowMouseCursor ? 1 : 0);
        if (AppUsesJoystick && (UseAccelerometerAsArrowKeys || UseTouchscreenKeyboard))
            nativeSetJoystickUsed();
        if (AppUsesAccelerometer)
            nativeSetAccelerometerUsed();
        if (AppUsesMultitouch)
            nativeSetMultitouchUsed();
        nativeSetAccelerometerSettings(AccelerometerSensitivity, AccelerometerCenterPos);
        nativeSetTrackballDampening(TrackballDampening);
        if (UseTouchscreenKeyboard) {
            boolean screenKbReallyUsed = false;
            for (int i = 0; i < ScreenKbControlsShown.length; i++)
                if (ScreenKbControlsShown[i])
                    screenKbReallyUsed = true;
            if (screenKbReallyUsed) {
                nativeSetTouchscreenKeyboardUsed();
                nativeSetupScreenKeyboard(TouchscreenKeyboardSize, TouchscreenKeyboardDrawSize,
                        TouchscreenKeyboardTheme, AppTouchscreenKeyboardKeysAmountAutoFire,
                        TouchscreenKeyboardTransparency);
                SetupTouchscreenKeyboardGraphics(mContext);
                for (int i = 0; i < ScreenKbControlsShown.length; i++)
                    nativeSetScreenKbKeyUsed(i, ScreenKbControlsShown[i] ? 1 : 0);
                for (int i = 0; i < RemapScreenKbKeycode.length; i++)
                    nativeSetKeymapKeyScreenKb(i, SDL_Keys.values[RemapScreenKbKeycode[i]]);
                for (int i = 0; i < ScreenKbControlsLayout.length; i++)
                    if (ScreenKbControlsLayout[i][0] < ScreenKbControlsLayout[i][2])
                        nativeSetScreenKbKeyLayout(i, ScreenKbControlsLayout[i][0],
                                ScreenKbControlsLayout[i][1], ScreenKbControlsLayout[i][2],
                                ScreenKbControlsLayout[i][3]);
            } else
                UseTouchscreenKeyboard = false;
        }

        for (int i = 0; i < SDL_Keys.JAVA_KEYCODE_LAST; i++)
            nativeSetKeymapKey(i, SDL_Keys.values[RemapHwKeycode[i]]);
        for (int i = 0; i < RemapMultitouchGestureKeycode.length; i++)
            nativeSetKeymapKeyMultitouchGesture(i,
                    MultitouchGesturesUsed[i] ? SDL_Keys.values[RemapMultitouchGestureKeycode[i]]
                            : 0);
        nativeSetMultitouchGestureSensitivity(MultitouchGestureSensitivity);
        if (TouchscreenCalibration[2] > TouchscreenCalibration[0])
            nativeSetTouchscreenCalibration(TouchscreenCalibration[0], TouchscreenCalibration[1],
                    TouchscreenCalibration[2], TouchscreenCalibration[3]);

        String lang = new String(Locale.getDefault().getLanguage());
        if (Locale.getDefault().getCountry().length() > 0)
            lang = lang + "_" + Locale.getDefault().getCountry();
        System.out.println("libSDL: setting envvar LANGUAGE to '" + lang + "'");
        nativeSetEnv("LANG", lang);
        nativeSetEnv("LANGUAGE", lang);
        // TODO: get current user name and set envvar USER, the API is not
        // availalbe on Android 1.6 so I don't bother with this
        nativeSetEnv("APPDIR", mContext.getFilesDir().getAbsolutePath());
        nativeSetEnv("SECURE_STORAGE_DIR", mContext.getFilesDir().getAbsolutePath());
        //nativeSetEnv("DATADIR", DataDir);
        //nativeSetEnv("UNSECURE_STORAGE_DIR", DataDir);
        //nativeSetEnv("HOME", DataDir);
        nativeSetEnv("ANDROID_VERSION", String.valueOf(android.os.Build.VERSION.SDK_INT));
        try {
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
            float xx = dm.widthPixels / dm.xdpi;
            float yy = dm.heightPixels / dm.ydpi;
            float x = Math.max(xx, yy);
            float y = Math.min(xx, yy);
            float displayInches = (float) Math.sqrt(x * x + y * y);
            nativeSetEnv("DISPLAY_SIZE", String.valueOf(displayInches));
            nativeSetEnv("DISPLAY_SIZE_MM", String.valueOf((int) (displayInches * 25.4f)));
            nativeSetEnv("DISPLAY_WIDTH", String.valueOf(x));
            nativeSetEnv("DISPLAY_HEIGHT", String.valueOf(y));
            nativeSetEnv("DISPLAY_WIDTH_MM", String.valueOf((int) (x * 25.4f)));
            nativeSetEnv("DISPLAY_HEIGHT_MM", String.valueOf((int) (y * 25.4f)));
            nativeSetEnv("DISPLAY_RESOLUTION_WIDTH",
                    String.valueOf(Math.max(dm.widthPixels, dm.heightPixels)));
            nativeSetEnv("DISPLAY_RESOLUTION_HEIGHT",
                    String.valueOf(Math.min(dm.widthPixels, dm.heightPixels)));
        } catch (Exception eeeee) {
        }
    }

    static byte[] loadRaw(Activity p, int res) {
        byte[] buf = new byte[65536 * 2];
        byte[] a = new byte[65536 * 4 * 10]; // We need 2363516 bytes for
                                             // the Sun theme
        int written = 0;
        try {
            InputStream is = new GZIPInputStream(p.getResources().openRawResource(res));
            int readed = 0;
            while ((readed = is.read(buf)) >= 0) {
                if (written + readed > a.length) {
                    byte[] b = new byte[written + readed];
                    System.arraycopy(a, 0, b, 0, written);
                    a = b;
                }
                System.arraycopy(buf, 0, a, written, readed);
                written += readed;
            }
        } catch (Exception e) {
        }
        ;
        byte[] b = new byte[written];
        System.arraycopy(a, 0, b, 0, written);
        return b;
    }

    public static void SetupTouchscreenKeyboardGraphics(Context context) {
        if (UseTouchscreenKeyboard) {
            if (TouchscreenKeyboardTheme < 0)
                TouchscreenKeyboardTheme = 0;
            if (TouchscreenKeyboardTheme > 2)
                TouchscreenKeyboardTheme = 2;
        }
    }

    abstract static class SdcardAppPath {
        private static Settings.SdcardAppPath get() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO)
                return Froyo.Holder.sInstance;
            else
                return Dummy.Holder.sInstance;
        }

        public abstract String path(final FFPlayer p);

        public static String deprecatedPath(final FFPlayer p) {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/app-data/"
                    + p.getPackageName();
        }

        public static String getPath(final FFPlayer p) {
            try {
                return get().path(p);
            } catch (Exception e) {
            }
            return Dummy.Holder.sInstance.path(p);
        }

        public static class Froyo extends Settings.SdcardAppPath {
            private static class Holder {
                private static final SdcardAppPath.Froyo sInstance = new Froyo();
            }

            @SuppressLint("NewApi")
            public String path(final FFPlayer p) {
                return p.getExternalFilesDir(null).getAbsolutePath();
            }
        }

        public static class Dummy extends Settings.SdcardAppPath {
            private static class Holder {
                private static final SdcardAppPath.Dummy sInstance = new Dummy();
            }

            public String path(final FFPlayer p) {
                return Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Android/data/" + p.getPackageName() + "/files";
            }
        }
    }

    static boolean checkRamSize(final FFPlayer p) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("MemTotal:") == 0) {
                    String[] fields = line.split("[ \t]+");
                    Long size = Long.parseLong(fields[1]);
                    System.out.println("Device RAM size: " + size / 1024
                            + " Mb, required minimum RAM: " + AppMinimumRAM + " Mb");
                    if (size / 1024 < AppMinimumRAM) {
                        settingsChanged = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(p);
                        builder.setTitle(R.string.not_enough_ram);
                        builder.setMessage(p.getResources().getString(R.string.not_enough_ram_size,
                                AppMinimumRAM, (int) (size / 1024)));
                        builder.setPositiveButton(p.getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        p.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                                .parse("market://details?id=" + p.getPackageName())));
                                        System.exit(0);
                                    }
                                });
                        builder.setNegativeButton(p.getResources().getString(R.string.ignore),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        showConfig(p, true);
                                        return;
                                    }
                                });
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                p.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                        .parse("market://details?id=" + p.getPackageName())));
                                System.exit(0);
                            }
                        });
                        final AlertDialog alert = builder.create();
                        alert.setOwnerActivity(p);
                        alert.show();
                        return false;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error: cannot parse /proc/meminfo: " + e.toString());
        }
        return true;
    }

    private static native void nativeSetTrackballUsed();

    private static native void nativeSetTrackballDampening(int value);

    private static native void nativeSetAccelerometerSettings(int sensitivity, int centerPos);

    private static native void nativeSetMouseUsed(int RightClickMethod, int ShowScreenUnderFinger,
            int LeftClickMethod, int MoveMouseWithJoystick, int ClickMouseWithDpad, int MaxForce,
            int MaxRadius, int MoveMouseWithJoystickSpeed, int MoveMouseWithJoystickAccel,
            int leftClickKeycode, int rightClickKeycode, int leftClickTimeout,
            int rightClickTimeout, int relativeMovement, int relativeMovementSpeed,
            int relativeMovementAccel, int showMouseCursor);

    private static native void nativeSetJoystickUsed();

    private static native void nativeSetAccelerometerUsed();

    private static native void nativeSetMultitouchUsed();

    private static native void nativeSetTouchscreenKeyboardUsed();

    private static native void nativeSetVideoLinearFilter();

    private static native void nativeSetVideoDepth(int bpp, int gles2);

    private static native void nativeSetCompatibilityHacks();

    private static native void nativeSetVideoMultithreaded();

    private static native void nativeSetVideoForceSoftwareMode();

    private static native void nativeSetupScreenKeyboard(int size, int drawsize, int theme,
            int nbuttonsAutoFire, int transparency);

    private static native void nativeSetupScreenKeyboardButtons(byte[] img);

    private static native void nativeInitKeymap();

    private static native int nativeGetKeymapKey(int key);

    private static native void nativeSetKeymapKey(int javakey, int key);

    private static native int nativeGetKeymapKeyScreenKb(int keynum);

    private static native void nativeSetKeymapKeyScreenKb(int keynum, int key);

    private static native void nativeSetScreenKbKeyUsed(int keynum, int used);

    private static native void nativeSetScreenKbKeyLayout(int keynum, int x1, int y1, int x2, int y2);

    private static native int nativeGetKeymapKeyMultitouchGesture(int keynum);

    private static native void nativeSetKeymapKeyMultitouchGesture(int keynum, int key);

    private static native void nativeSetMultitouchGestureSensitivity(int sensitivity);

    private static native void nativeSetTouchscreenCalibration(int x1, int y1, int x2, int y2);

    public static native void nativeSetEnv(final String name, final String value);

    public static native int nativeChmod(final String name, int mode);

    public static int           AccelerometerCenterPos                        = 2;

    public static int           AccelerometerSensitivity                      = 2;

    public static String        AdmobBannerSize                               = "";

    public static String        AdmobPublisherId                              = "";

    public static String        AdmobTestDeviceId                             = "";

    public static String        AppLibraries[]                                = { "sdl-1.2",
            "avutil", "avcodec", "avformat", "avdevice", "swscale", "swresample", "avresample",
            "avfilter"                                                       };

    // These config options are modified by ChangeAppsettings.sh script - see
    // the detailed descriptions there
    public static String        ApplicationName                               = "FFPlayer";

    public static int           AppMinimumRAM                                 = 0;

    public static boolean       AppNeedsArrowKeys                             = true;

    public static boolean       AppNeedsTextInput                             = false;

    public static boolean       AppNeedsTwoButtonMouse                        = false;

    public static int           AppTouchscreenKeyboardKeysAmount              = 0;

    public static int           AppTouchscreenKeyboardKeysAmountAutoFire      = 0;

    public static String[]      AppTouchscreenKeyboardKeysNames               = "0 1 2 3 4 5 6 7 8 9"
                                                                                      .split(" ");

    public static boolean       AppUsesAccelerometer                          = false;

    public static boolean       AppUsesGyroscope                              = false;

    public static boolean       AppUsesJoystick                               = false;

    public static boolean       AppUsesMouse                                  = false;

    public static boolean       AppUsesMultitouch                             = false;

    public static int           AudioBufferConfig                             = 0;

    public static boolean       BrokenLibCMessageShown                        = false;

    public static boolean       ClickMouseWithDpad                            = false;

    public static int           ClickScreenPressure                           = 0;

    public static int           ClickScreenTouchspotSize                      = 0;

    public static String        CommandLine                                   = "ffplay -loglevel verbose -nostats";

    public static boolean       CompatibilityHacksStaticInit                  = false;

    public static boolean       CompatibilityHacksTextInputEmulatesHwKeyboard = false;

    public static boolean       CompatibilityHacksVideo                       = false;

    public static String        DeleteFilesOnUpgrade                          = "%";

    // Phone-specific config, modified by user in "Change phone config" startup
    // dialog, TODO: move this to settings

    public static boolean       ForceRelativeMouseMode                        = false;                                        // If
                                                                                                                               // both
                                                                                                                               // on-screen

    public static Settings.Menu FirstStartMenuOptions[]                       = {
            (AppUsesMouse && !ForceRelativeMouseMode ? new Settings.DisplaySizeConfig(true)
                    : new Settings.DummyMenu()), new Settings.GyroscopeCalibration() };

    public static Settings.Menu HiddenMenuOptions[]                           = {};

    public static boolean       HorizontalOrientation                         = true;

    public static boolean       InhibitSuspend                                = true;

    public static boolean       KeepAspectRatioDefaultSetting                 = false;

    public static boolean       KeepAspectRatio                               = KeepAspectRatioDefaultSetting;

    public static int           LeftClickKey                                  = KeyEvent.KEYCODE_DPAD_CENTER;

    public static int           LeftClickMethod                               = SDLInput.Mouse.LEFT_CLICK_NORMAL;

    public static int           LeftClickTimeout                              = 3;

    public static boolean       MoveMouseWithJoystick                         = false;

    public static int           MoveMouseWithJoystickAccel                    = 0;

    public static int           MoveMouseWithJoystickSpeed                    = 0;

    public static boolean       MultiThreadedVideo                            = false;

    public static int           MultitouchGestureSensitivity                  = 1;

    public static boolean       MultitouchGesturesUsed[]                      = new boolean[4];

    public static boolean       NeedDepthBuffer                               = false;

    public static boolean       NeedGles2                                     = false;

    public static boolean       NeedStencilBuffer                             = false;

    public static boolean       NonBlockingSwapBuffers                        = false;

    public static boolean       PhoneHasArrowKeys                             = false;

    public static boolean       PhoneHasTrackball                             = false;

    public static String        ReadmeText                                    = "^Readme text";

    public static boolean       RelativeMouseMovement                         = ForceRelativeMouseMode;                       // Laptop

    public static int           RelativeMouseMovementAccel                    = 0;

    // touchpad
    // mode
    public static int           RelativeMouseMovementSpeed                    = 2;

    public static int           RemapHwKeycode[]                              = new int[SDL_Keys.JAVA_KEYCODE_LAST];

    public static int           RemapMultitouchGestureKeycode[]               = new int[4];

    public static int           RemapScreenKbKeycode[]                        = new int[6];

    public static boolean       ResetSdlConfigForThisVersion                  = false;

    public static int           RightClickKey                                 = KeyEvent.KEYCODE_MENU;

    public static int           RightClickMethod                              = AppNeedsTwoButtonMouse ? SDLInput.Mouse.RIGHT_CLICK_WITH_MULTITOUCH
                                                                                      : SDLInput.Mouse.RIGHT_CLICK_NONE;

    public static int           RightClickTimeout                             = 4;

    /*
     * Also joystick and text input button added
     */
    public static int           ScreenKbControlsLayout[][]                    = new int[0][0];

    public static boolean       ScreenKbControlsShown[]                       = new boolean[8];

    // keyboard and
    // mouse are needed,
    // this will only
    // set the default
    // setting, user may
    // override it later
    public static boolean       ShowMouseCursor                               = false;

    public static int           ShowScreenUnderFinger                         = SDLInput.Mouse.ZOOM_NONE;

    public static int           StartupMenuButtonTimeout                      = 0;

    public static boolean       SwVideoMode                                   = true;

    public static int           TouchscreenCalibration[]                      = new int[4];

    public static int           TouchscreenKeyboardDrawSize                   = 1;

    public static int           TouchscreenKeyboardSize                       = 1;

    public static int           TouchscreenKeyboardTheme                      = 2;

    public static int           TouchscreenKeyboardTransparency               = 2;

    public static int           TrackballDampening                            = 0;

    public static boolean       UseAccelerometerAsArrowKeys                   = false;

    public static boolean       UseTouchscreenKeyboard                        = true;

    public static final boolean Using_SDL_1_3                                 = false;

    public static int           VideoDepthBpp                                 = 24;

    public static boolean       VideoLinearFilter                             = true;
}