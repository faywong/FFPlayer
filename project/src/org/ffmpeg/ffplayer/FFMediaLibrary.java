package org.ffmpeg.ffplayer;

import io.github.faywong.ffplayer.R;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import org.ffmpeg.ffplayer.util.MusicUtils;

public class FFMediaLibrary extends ListActivity implements MusicUtils.Defs,
        OnItemSelectedListener, LoaderCallbacks<Cursor> {
    private static final String   TAG                     = "FFMediaLibrary";
    private static final String[] displayColumnCandidates = { MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, };

    private String                targetProtocol;
    private Spinner               protocol_pinner;
    private EditText              url_portion;

    private Cursor                mCursor;
    private String                mSortOrder;
    private String                displayColumn           = MediaStore.Video.Media.DISPLAY_NAME;
    private SimpleCursorAdapter   mAdapter;
    private BroadcastReceiver mediaScannerReceiver;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean("first_time", false))
        {
            Intent i = new Intent(FFMediaLibrary.this, AboutActivity.class);
            this.startActivity(i);
            this.finish();
        }
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
                    Toast.makeText(FFMediaLibrary.this, "You need to input a valid url!",
                            Toast.LENGTH_SHORT).show();
                    ;
                    return;
                }
                Log.d(TAG, "onClick() in check point 2");

                String network_src_url = targetProtocol + url_remaining_portion;
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

        final ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null,
                null, null);
        for (String candidate : displayColumnCandidates) {
            if (-1 != cursor.getColumnIndex(candidate)) {
                displayColumn = candidate;
                break;
            }
        }
        getLoaderManager().initLoader(0, null, this);

        // Create a progress bar to display while the list loads
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);

        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(progressBar);

        // Map Cursor columns to views defined in media_list_item.xml
        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null,
                new String[] { displayColumn }, new int[] { android.R.id.text1 }, 0);

        setListAdapter(mAdapter);
        
        mediaScannerReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                final String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    return;
                }
                if (TextUtils.equals(action, Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                    Loader<Cursor> oldLoader = getLoaderManager().getLoader(0);
                    if (oldLoader != null) {
                        oldLoader.forceLoad();
                    }
                }
            }
            
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        registerReceiver(mediaScannerReceiver, filter);
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

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
        unregisterReceiver(mediaScannerReceiver);
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // TODO Auto-generated method stub
        targetProtocol = (String) parent.getItemAtPosition(pos);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
        protocol_pinner.setSelection(0, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO Auto-generated method stub
        String[] cols = new String[] { MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST };

        StringBuilder sb = new StringBuilder();
        sb.append(MediaStore.Video.Media.TITLE + " != '' OR ");
        sb.append(displayColumn + " != ''");

        String whereClause = sb.toString();
        mSortOrder = MediaStore.Video.Media.DISPLAY_NAME + " COLLATE UNICODE";
        return new CursorLoader(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols,
                whereClause, null, mSortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // TODO Auto-generated method stub
        mCursor = data;
        if (mCursor != null && mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }
        mAdapter.swapCursor(mCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO Auto-generated method stub
        mAdapter.swapCursor(null);
    }
}
