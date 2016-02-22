package org.ginatrapani.cinefile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.ginatrapani.cinefile.data.FetchMoviesTask;
import org.ginatrapani.cinefile.data.ImageAdapter;
import org.ginatrapani.cinefile.data.Movie;

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

        View rootView = inflater.inflate(R.layout.fragment_poster_grid, container, false);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        mMoviesAdapter = new ImageAdapter(this.getActivity());
        gridview.setAdapter(mMoviesAdapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Movie clickedMovie = (Movie) mMoviesAdapter.getItem(position);

                ((MovieDetailActivityFragment.Callback) getActivity())
                        .setMovie(clickedMovie);
                ((MovieDetailActivityFragment.Callback) getActivity())
                        .onItemSelected();
            }
        });
        return rootView;
    }

    private void updateMovies() {
        if (mMovies != null) {
            mMoviesAdapter.setMovies(mMovies);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortOrder = prefs.getString(getString(R.string.pref_key_sort),
                    getString(R.string.pref_default_sort));
            new FetchMoviesTask(getActivity(), mMoviesAdapter ).execute(sortOrder);
        }
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
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_MOVIE_LIST, mMoviesAdapter.getMovies());
        super.onSaveInstanceState(outState);
    }
}
