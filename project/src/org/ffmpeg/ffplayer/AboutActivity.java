package org.ffmpeg.ffplayer;

import io.github.faywong.ffplayer.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AboutActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
        if (findViewById(R.id.fragment_container) != null) {
            String aboutUrl = "file:///android_asset/about_me.html";
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, AboutFragment.newInstance(aboutUrl)).commit();
        }
        Button confirmBtn = (Button) findViewById(R.id.confirm);
        confirmBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent();
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(AboutActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("first_time", true);
                editor.commit();
                intent.setClass(getApplicationContext(), FFMediaLibrary.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
