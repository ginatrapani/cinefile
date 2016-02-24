package org.ginatrapani.cinefile;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.support.v4.content.CursorLoader;
import org.ginatrapani.cinefile.data.FetchMoviesTask;
import org.ginatrapani.cinefile.data.ImageAdapter;
import org.ginatrapani.cinefile.data.Movie;
import org.ginatrapani.cinefile.data.MovieContract;

import java.util.ArrayList;

public class PosterGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static ImageAdapter mMoviesAdapter;

    private static final String KEY_MOVIE_LIST = "movies";

    private static final int MOVIES_LOADER = 0;

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

        View rootView = inflater.inflate(R.layout.fragment_poster_grid, container, false);
        GridView gridView = (GridView) rootView.findViewById(R.id.gridview);
        mMoviesAdapter = new ImageAdapter(this.getActivity(), null, 0);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIES_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
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
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
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

        return new CursorLoader(getActivity(),
                moviesUri,
                null,
                null,
                null,
                sortOrder);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mMoviesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mMoviesAdapter.swapCursor(null);
    }
}
