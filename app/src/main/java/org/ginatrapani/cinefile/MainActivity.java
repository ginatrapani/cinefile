package org.ginatrapani.cinefile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.ginatrapani.cinefile.data.FetchMoviesTask;

public class MainActivity extends AppCompatActivity implements MovieDetailActivityFragment.Callback {

    private boolean mTwoPane;

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private Uri mMovieUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (findViewById(R.id.movie_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, new MovieDetailActivityFragment(),
                                DETAILFRAGMENT_TAG).commit();
            }
            (this).onItemSelected();
        } else {
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if ( id == R.id.action_refresh) {
            updateMovies();
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMovies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sortOrder = prefs.getString(getString(R.string.pref_key_sort),
                getString(R.string.pref_default_sort));
        new FetchMoviesTask(this).execute(sortOrder);
    }

    @Override
    public void setMovieUri(Uri movieUri) {
        mMovieUri = movieUri;
    }

    @Override
    public void onMovieListLoaded(Uri firstMovieUri) {
        //Log.v(LOG_TAG, "onMovieListLoaded called");
        if (mTwoPane) {
            //Log.v(LOG_TAG, "In a 2-pane layout");
            setMovieUri(firstMovieUri);
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();

            MovieDetailActivityFragment fragment = new MovieDetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void onItemSelected() {
        //Log.v(LOG_TAG, "onItemSelected called");
        if (mTwoPane) {
            //Log.v(LOG_TAG, "In a 2-pane layout");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();

            MovieDetailActivityFragment fragment = new MovieDetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            //Log.v(LOG_TAG, "In a single pane layout");
            if (mMovieUri != null) {
                //Log.v(LOG_TAG, "There's a movie to show");
                Intent intent = new Intent(this, MovieDetailActivity.class).setData(mMovieUri);
                startActivity(intent);
            }
        }
    }
}
