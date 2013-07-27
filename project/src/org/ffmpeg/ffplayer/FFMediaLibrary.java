package org.ffmpeg.ffplayer;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import org.ffmpeg.ffplayer.util.MusicUtils;

public class FFMediaLibrary extends ListActivity implements MusicUtils.Defs, OnItemSelectedListener  {
    private static final String   TAG           = "FFMediaLibrary";
    private static final String[] video_filters = { MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, };

    private String target_protocol;
    private Spinner protocol_pinner;
    private EditText url_portion;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        init();
    }

    public void init() {

        // Set the layout for this activity. You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);

        protocol_pinner = (Spinner) findViewById(R.id.network_protocol_spinner);
        // Create an ArrayAdapter using the string array and a default spinner
        // layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.protocols, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        protocol_pinner.setAdapter(adapter);
        protocol_pinner.setOnItemSelectedListener(this);
        url_portion = (EditText) findViewById(R.id.url_portion);

        Button netwok_play_btn = (Button) findViewById(R.id.network_play_btn);
        netwok_play_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.d(TAG, "onClick() in" + url_portion);
                if (null == url_portion) {
                    return;
                }
                Log.d(TAG, "onClick() in check point 1");

                String url_remaining_portion = url_portion.getText().toString();
                if (url_remaining_portion != null && url_remaining_portion.isEmpty()) {
                    Toast.makeText(FFMediaLibrary.this, "You need to input a valid url!", Toast.LENGTH_SHORT);
                    return;
                }
                Log.d(TAG, "onClick() in check point 2");

                String network_src_url = target_protocol + url_remaining_portion;
                Log.d(TAG, "The target network src url:" + network_src_url);
                Intent launchFFPlayer = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(network_src_url);
                launchFFPlayer.setData(uri);
                Log.d(TAG, "The target network src uri:" + uri);
                Log.d(TAG, "onClick() in check point 3");

                launchFFPlayer.setClass(getApplicationContext(), FFPlayer.class);
                try {
                    FFMediaLibrary.this.startActivity(launchFFPlayer);
                } catch (ActivityNotFoundException ex) {
                    Log.e(TAG, "FATAL ERROR! The target FFPlayer activity is not found!");
                    ex.printStackTrace();
                }
            }
        });

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
        SimpleCursorAdapter protocolAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, mCursor, new String[] { mDisplayName },
                new int[] { android.R.id.text1 });

        setListAdapter(protocolAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        mCursor.moveToPosition(position);
        String type = mCursor.getString(mCursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        intent.setDataAndType(
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        intent.setClass(getApplicationContext(), FFPlayer.class);
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = new String[] { MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST };
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            Log.d(TAG, "resolver = null");
        } else {
            for (String filter_key : video_filters) {
                String whereClause = filter_key + " != ''";
                mSortOrder = MediaStore.Video.Media.DISPLAY_NAME + " COLLATE UNICODE";
                /*
                mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols,
                        whereClause, null, mSortOrder);
                */
                CursorLoader loader = new CursorLoader(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, whereClause, null, mSortOrder);
                mCursor = loader.loadInBackground();

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // TODO Auto-generated method stub
        target_protocol = (String)parent.getItemAtPosition(pos);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
        protocol_pinner.setSelection(0, true);
    }
}
