package org.ginatrapani.cinefile;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.ginatrapani.cinefile.data.FetchMoviesTask;
import org.ginatrapani.cinefile.data.ImageAdapter;
import org.ginatrapani.cinefile.data.Movie;
import org.ginatrapani.cinefile.data.MovieContract;

import java.util.ArrayList;

public class PosterGridFragment extends Fragment {

    public static ImageAdapter mMoviesAdapter;

    private static final String KEY_MOVIE_LIST = "movies";

    private ArrayList<Movie> mMovies;

    public PosterGridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.poster_grid_fragment_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // read the movie list from the saved state
            mMovies = savedInstanceState.getParcelableArrayList(KEY_MOVIE_LIST);
        }

        // Sort order:  vote_average.desc, popularity.desc, or favorites.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = prefs.getString(getActivity().getString(R.string.pref_key_sort),
                getActivity().getString(R.string.pref_default_sort));

        if (sortOrder.equals("vote_average.desc")) {
            sortOrder = MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE;
        } else if (sortOrder.equals("popularity.desc")) {
            sortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY;
        } else if (sortOrder.equals("favorites")) {
            // @TODO Do the join on the future favorites table
            sortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY; //
        }
        sortOrder += " DESC";
        Uri moviesUri = MovieContract.MovieEntry.CONTENT_URI;

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert
        Cursor cur = getActivity().getContentResolver().query(moviesUri, null, null, null,
                sortOrder);

        View rootView = inflater.inflate(R.layout.fragment_poster_grid, container, false);
        GridView gridView = (GridView) rootView.findViewById(R.id.gridview);
        mMoviesAdapter = new ImageAdapter(this.getActivity(), cur, 0);
        gridView.setAdapter(mMoviesAdapter);

        return rootView;
    }

    private void updateMovies() {
//        if (mMovies != null) {
//            mMoviesAdapter.setMovies(mMovies);
//        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortOrder = prefs.getString(getString(R.string.pref_key_sort),
                    getString(R.string.pref_default_sort));
            new FetchMoviesTask(getActivity()).execute(sortOrder);
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //outState.putParcelableArrayList(KEY_MOVIE_LIST, mMoviesAdapter.getMovies());
        super.onSaveInstanceState(outState);
    }
}
