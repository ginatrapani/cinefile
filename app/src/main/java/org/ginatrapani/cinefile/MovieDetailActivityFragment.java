package org.ginatrapani.cinefile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailActivityFragment extends Fragment {
    private static final String LOG_TAG = "TAG";

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // The detail Activity called via intent.  Inspect the intent for movie data.
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String detailStr = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.v(LOG_TAG, "Detail string is " + detailStr);
            ((TextView) rootView.findViewById(R.id.detail_text))
                    .setText(detailStr);
        } else {
            if (intent == null) {
                Log.v(LOG_TAG, "Intent is null");
            } else {
                Log.v(LOG_TAG, "Intent has no extra");
            }
            ((TextView) rootView.findViewById(R.id.detail_text))
                    .setText("HAI");
        }

        return rootView;
    }
}
