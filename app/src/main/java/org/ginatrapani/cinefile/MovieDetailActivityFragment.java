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
            ((TextView) rootView.findViewById(R.id.movie_release))
                    .setText(movie.getReleaseDate());
            ((TextView) rootView.findViewById(R.id.vote_average))
                    .setText(movie.getVoteAverage());
            ImageView imageView = (ImageView) rootView.findViewById(R.id.movie_poster);
            Picasso.with(getActivity()).load(movie.getPosterPath()).
                    resize(movie.getDefaultWidth() * 3, movie.getDefaultHeight() * 3)
                    .into(imageView);

        }
        return rootView;
    }
}
