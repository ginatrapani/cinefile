package org.ginatrapani.cinefile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class MovieDetailActivityFragment extends Fragment {

    //private final String LOG_TAG = MovieDetailActivityFragment.class.getSimpleName();

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // The detail Activity called via intent.  Inspect the intent for movie data.
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("movie")) {
            Movie movie = intent.getParcelableExtra("movie");
            //Log.v(TAG, "Overview string is " + movie.getOverview());
            ((TextView) rootView.findViewById(R.id.detail_text))
                    .setText(movie.getOverview());
            ((TextView) rootView.findViewById(R.id.movie_title))
                    .setText(movie.getTitle());
            SimpleDateFormat apiReleaseDate = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat friendlyReleaseDateFormat = new SimpleDateFormat("MMMM d, yyyy");

            String reformattedReleaseDateStr = movie.getReleaseDate();
            try {
                reformattedReleaseDateStr = friendlyReleaseDateFormat
                        .format(apiReleaseDate.parse(movie.getReleaseDate()));
            } catch (ParseException e) {
                //do nothing, default to what API returned
            }
            ((TextView) rootView.findViewById(R.id.movie_release))
                    .setText(reformattedReleaseDateStr);

            String voteAverageString = movie.getVoteAverage()
                    + " " + getResources().getString(R.string.vote_average_suffix);
            ((TextView) rootView.findViewById(R.id.vote_average))
                    .setText(voteAverageString);
            ImageView imageView = (ImageView) rootView.findViewById(R.id.movie_poster);
            Picasso.with(getActivity()).load(movie.getPosterPath())
                    .into(imageView);
        }
        return rootView;
    }
}
