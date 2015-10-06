package org.ginatrapani.cinefile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity implements MovieDetailActivityFragment.Callback {

    private boolean mTwoPane;

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private Movie mMovie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            // read the movie list from the saved state
            mMovie = savedInstanceState.getParcelable(MovieDetailActivityFragment.MOVIE);
        }
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
            ((MovieDetailActivityFragment.Callback) this).onItemSelected();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setMovie(Movie movie) {
        mMovie = movie;
    }

    @Override
    public void onItemSelected() {
        Log.v(LOG_TAG, "onItemSelected called");
        if (mTwoPane) {
            Log.v(LOG_TAG, "In a 2-pane layout");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(MovieDetailActivityFragment.MOVIE, mMovie);

            MovieDetailActivityFragment fragment = new MovieDetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Log.v(LOG_TAG, "In a single pane layout");
            if (mMovie != null) {
                Log.v(LOG_TAG, "There's a movie to show");
                Intent intent = new Intent(this, MovieDetailActivity.class)
                        .putExtra(MovieDetailActivityFragment.MOVIE, mMovie);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMovie != null) {
            Log.v(LOG_TAG, "Saving movie to outstate");
            outState.putParcelable(MovieDetailActivityFragment.MOVIE, mMovie);
        }
        super.onSaveInstanceState(outState);
    }
}
