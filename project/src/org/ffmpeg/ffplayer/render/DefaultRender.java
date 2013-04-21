package org.ffmpeg.ffplayer.render;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import org.ffmpeg.ffplayer.config.Globals;
import org.ffmpeg.ffplayer.config.Settings;
import org.ffmpeg.ffplayer.render.FFPlayerView.AdHelper;
import org.ffmpeg.ffplayer.render.FFPlayerView.AppHelper;
import org.ffmpeg.ffplayer.render.FFPlayerView.ScreenKeyboardHelper;
import org.ffmpeg.ffplayer.util.AccelerometerReader;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;

import org.ffmpeg.ffplayer.R;

public class DefaultRender extends GLSurfaceView_SDL.Renderer {
	private ScreenKeyboardHelper mKeyboardHelper;
	private AdHelper mAdHelper;
	private AppHelper mAppHelper;
	private Context mContext = null;
	public AccelerometerReader accelerometer = null;

	private GL10 mGl = null;
	private EGL10 mEgl = null;
	private EGLDisplay mEglDisplay = null;
	private EGLSurface mEglSurface = null;
	private EGLContext mEglContext = null;
	private boolean mGlContextLost = false;
	public boolean mGlSurfaceCreated = false;
	public boolean mPaused = false;
	private int mState = GLSurfaceView_SDL.Renderer.STATE_PAUSED;
	private boolean mFirstTimeStart = true;
	public int mWidth = 0;
	public int mHeight = 0;

	public static final boolean mRatelimitTouchEvents = true; // (Build.VERSION.SDK_INT
																// >=
																// Build.VERSION_CODES.FROYO);

	private boolean paused() {
		return mState == GLSurfaceView_SDL.Renderer.STATE_PAUSED;
	}

	public DefaultRender(Context context,
			ScreenKeyboardHelper keyboardHelper, AdHelper adHelper,
			AppHelper appHelper) {
		mContext = context;
		mKeyboardHelper = keyboardHelper;
		mAdHelper = adHelper;
		mAppHelper = appHelper;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		System.out
				.println("libSDL: DefaultRenderer.onSurfaceCreated(): paused "
						+ paused() + " mFirstTimeStart " + mFirstTimeStart);
		mGlSurfaceCreated = true;
		mGl = gl;
		if (!paused() && !mFirstTimeStart)
			nativeGlContextRecreated();
		mFirstTimeStart = false;
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		System.out
				.println("libSDL: DefaultRenderer.onSurfaceChanged(): paused "
						+ paused() + " mFirstTimeStart " + mFirstTimeStart);
		mWidth = w;
		mHeight = h;
		mGl = gl;
		nativeResize(w, h, Globals.KeepAspectRatio ? 1 : 0);
	}

	public void onSurfaceDestroyed() {
		System.out
				.println("libSDL: DefaultRenderer.onSurfaceDestroyed(): paused "
						+ paused() + " mFirstTimeStart " + mFirstTimeStart);
		mGlSurfaceCreated = false;
		mGlContextLost = true;
		nativeGlContextLost();
	};

	public void onDrawFrame(GL10 gl) {

		mGl = gl;
		DrawLogo(mGl);
		SwapBuffers();

		nativeInitJavaCallbacks();

		// Make main thread priority lower so audio thread won't get
		// underrun
		// Thread.currentThread().setPriority((Thread.currentThread().getPriority()
		// + Thread.MIN_PRIORITY)/2);

		mGlContextLost = false;

		if (Globals.CompatibilityHacksStaticInit)
			mAppHelper.LoadApplicationLibrary(mContext);

		Settings.Apply(mContext);
		accelerometer = new AccelerometerReader(mContext);
		// Tweak video thread priority, if user selected big audio buffer
		if (Globals.AudioBufferConfig >= 2)
			Thread.currentThread().setPriority(
					(Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2); // Lower
																		// than
																		// normal
		// Calls main() and never returns, hehe - we'll call
		// eglSwapBuffers() from native code
		nativeInit(
				Globals.DataDir,
				Globals.CommandLine,
				((Globals.SwVideoMode && Globals.MultiThreadedVideo) || Globals.CompatibilityHacksVideo) ? 1
						: 0, android.os.Debug.isDebuggerConnected() ? 1 : 0);
		System.exit(0); // The main() returns here - I don't bother with
						// deinit stuff, just terminate process
	}

	public int swapBuffers() // Called from native code
	{
		if (!super.SwapBuffers() && Globals.NonBlockingSwapBuffers) {
			if (mRatelimitTouchEvents) {
				synchronized (this) {
					this.notify();
				}
			}
			return 0;
		}

		if (mGlContextLost) {
			mGlContextLost = false;
			Settings.SetupTouchscreenKeyboardGraphics(mContext); // Reload
																// on-screen
																// buttons
																// graphics
			DrawLogo(mGl);
			super.SwapBuffers();
		}

		// Unblock event processing thread only after we've finished
		// rendering
		if (mRatelimitTouchEvents) {
			synchronized (this) {
				this.notify();
			}
		}
		if (mKeyboardHelper.isScreenKeyboardShown()) {
			try {
				Thread.sleep(50); // Give some time to the keyboard input
									// thread
			} catch (Exception e) {
			}
			;
		}
		return 1;
	}

	public void showScreenKeyboardWithoutTextInputField() // Called from
															// native code
	{
		class Callback implements Runnable {
			public ScreenKeyboardHelper mKeyboardHelper;

			public void run() {
				mKeyboardHelper.showScreenKeyboardWithoutTextInputField();
			}
		}
		Callback cb = new Callback();
		cb.mKeyboardHelper = mKeyboardHelper;
		((Activity) mContext).runOnUiThread(cb);
	}

	public void showScreenKeyboard(final String oldText, int sendBackspace) // Called
																			// from
																			// native
																			// code
	{
		class Callback implements Runnable {
			public ScreenKeyboardHelper mKeyboardHelper;
			public String oldText;
			public boolean sendBackspace;

			public void run() {
				mKeyboardHelper.showScreenKeyboard(oldText, sendBackspace);
			}
		}
		Callback cb = new Callback();
		cb.mKeyboardHelper = mKeyboardHelper;
		cb.oldText = oldText;
		cb.sendBackspace = (sendBackspace != 0);
		((Activity) mContext).runOnUiThread(cb);
	}

	public void hideScreenKeyboard() // Called from native code
	{
		class Callback implements Runnable {
			public ScreenKeyboardHelper mKeyboardHelper;

			public void run() {
				mKeyboardHelper.hideScreenKeyboard();
			}
		}
		Callback cb = new Callback();
		cb.mKeyboardHelper = mKeyboardHelper;
		((Activity) mContext).runOnUiThread(cb);
	}

	public int isScreenKeyboardShown() // Called from native code
	{
		if (null != mKeyboardHelper) {
			return mKeyboardHelper.isScreenKeyboardShown() ? 1 : 0;
		} else {
			return 0;
		}
	}

	public void setScreenKeyboardHintMessage(String s) {
		if (null != mKeyboardHelper) {
			mKeyboardHelper.setScreenKeyboardHintMessage(s);
		}
	}

	public void startAccelerometerGyroscope(int started) {
		accelerometer.openedBySDL = (started != 0);
		if (accelerometer.openedBySDL && !paused())
			accelerometer.start();
		else
			accelerometer.stop();
	}

	public void exitApp() {
		nativeDone();
	}

	public void getAdvertisementParams(int params[]) {
		if (null != mAdHelper) {
			mAdHelper.getAdvertisementParams(params);
		}
	}

	public void setAdvertisementVisible(int visible) {
		if (null != mAdHelper) {
			mAdHelper.setAdvertisementVisible(visible);
		}
	}

	public void setAdvertisementPosition(int left, int top) {
		if (null != mAdHelper) {
			mAdHelper.setAdvertisementPosition(left, top);
		}
	}

	public void requestNewAdvertisement() {
		if (null != mAdHelper) {
			mAdHelper.requestNewAdvertisement();
		}
	}

	private int PowerOf2(int i) {
		int value = 1;
		while (value < i)
			value <<= 1;
		return value;
	}

	public void DrawLogo(GL10 gl) {
		// TODO: this not quite works, as it seems
		BitmapDrawable bmp = null;
		try {
			bmp = new BitmapDrawable(mContext.getAssets().open("logo.png"));
		} catch (Exception e) {
			bmp = new BitmapDrawable(mContext.getResources()
					.openRawResource(R.drawable.publisherlogo));
		}
		int width = bmp.getBitmap().getWidth();
		int height = bmp.getBitmap().getHeight();
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * width
				* height);
		// byteBuffer.order(ByteOrder.BIG_ENDIAN);
		bmp.getBitmap().copyPixelsToBuffer(byteBuffer);
		byteBuffer.position(0);

		gl.glViewport(0, 0, mWidth, mHeight);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
		gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
		gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		int textureName = -1;
		int mTextureNameWorkspace[] = new int[1];
		int mCropWorkspace[] = new int[4];
		gl.glGenTextures(1, mTextureNameWorkspace, 0);
		textureName = mTextureNameWorkspace[0];
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName);
		gl.glActiveTexture(textureName);
		gl.glClientActiveTexture(textureName);
		gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA,
				PowerOf2(width), PowerOf2(height), 0, GL10.GL_RGBA,
				GL10.GL_UNSIGNED_BYTE, null);
		gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, width, height,
				GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, byteBuffer);
		mCropWorkspace[0] = 0; // u
		mCropWorkspace[1] = height; // v
		mCropWorkspace[2] = width;
		mCropWorkspace[3] = -height;
		((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D,
				GL11Ext.GL_TEXTURE_CROP_RECT_OES, mCropWorkspace, 0);
		((GL11Ext) gl).glDrawTexiOES(0, -mHeight, 0, mWidth, mHeight);
		gl.glActiveTexture(0);
		gl.glClientActiveTexture(0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
		gl.glDeleteTextures(1, mTextureNameWorkspace, 0);

		gl.glFlush();
	}

	private native void nativeInitJavaCallbacks();

	private native void nativeInit(String CurrentPath, String CommandLine,
			int multiThreadedVideo, int isDebuggerConnected);

	private native Parcel invoke(int action, Parcel request);

	private native Parcel invokeAync(int action, Parcel request);

	private native void nativeResize(int w, int h, int keepAspectRatio);

	private native void nativeDone();

	private native void nativeGlContextLost();

	public native void nativeGlContextRecreated();

	public native void nativeGlContextLostAsyncEvent();

	public static native void nativeTextInput(int ascii, int unicode);

	public static native void nativeTextInputFinished();

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return mState;
	}

	@Override
	public void setState(int state) {
		mState = state;
	}

}