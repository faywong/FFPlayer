
package org.ffmpeg.ffplayer;

import io.github.faywong.ffplayer.R;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutFragment extends Fragment {
    AboutActivity mContext;
    WebView howTo;

    public AboutFragment() {
        
    }
    
    public static Fragment newInstance(String arg) {
        AboutFragment fragment = new AboutFragment();
        Bundle bundle = new Bundle();
        bundle.putString("content-url", arg);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mContext = (AboutActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        howTo = (WebView) getView().findViewById(R.id.howto);
        howTo.setWebViewClient(new MyBrowser());
        howTo.getSettings().setLoadsImagesAutomatically(true);
        howTo.getSettings().setJavaScriptEnabled(true);
        howTo.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        howTo.loadUrl(getArguments().getString("content-url"));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        return (View) inflater.inflate(R.layout.about_fragment, null);
    }

    private class MyBrowser extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        return super.onOptionsItemSelected(item);
    }

}
