package org.ffmpeg.ffplayer;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.ffmpeg.ffplayer.util.MusicUtils;

public class FFMediaLibrary extends ListActivity implements MusicUtils.Defs
{
    private static final String TAG = "FFMediaLibrary";
    private static final String[] video_filters = {
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DISPLAY_NAME,
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        init();
    }

    public void init() {

        // Set the layout for this activity.  You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);

        MakeCursor();

        if (mCursor == null) {
            MusicUtils.displayDatabaseError(this);
            return;
        }
        Log.d(TAG, "mCursor " + mCursor + " count:" + mCursor.getCount());

        if (mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }

        // Map Cursor columns to views defined in media_list_item.xml
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                mCursor,
                new String[] { mDisplayName },
                new int[] { android.R.id.text1 });

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        mCursor.moveToPosition(position);
        String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        intent.setClass(getApplicationContext(), FFPlayer.class);
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST
        };
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            Log.d(TAG, "resolver = null");
        } else {
            for (String filter_key : video_filters) {
                String whereClause = filter_key + " != ''";
                mSortOrder = MediaStore.Video.Media.DISPLAY_NAME + " COLLATE UNICODE";
                mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols,
                        whereClause, null, mSortOrder);
                if (mCursor != null && mCursor.getCount() > 0) {
                    mDisplayName = filter_key;
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
        super.onDestroy();
    }

    private Cursor mCursor;
    private String mSortOrder;
    private String mDisplayName = MediaStore.Video.Media.DISPLAY_NAME;
}

