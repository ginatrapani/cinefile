package org.ginatrapani.cinefile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.ginatrapani.cinefile.sync.CinefileSyncAdapter;

public class MainActivity extends AppCompatActivity implements PosterGridFragment.Callback {

    private boolean mTwoPane;

    private static final String DETAIL_FRAGMENT_TAG = "DFTAG";

    private static final String POSTER_GRID_FRAGMENT_TAG = "PGFTAG";

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private Uri mMovieUri;

    private String mApiSortOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "In onCreate");
        super.onCreate(savedInstanceState);

        mApiSortOrder = Utility.getAPISortOrder(this);

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
                                DETAIL_FRAGMENT_TAG).commit();
            }
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
        //@TODO Delete this once updates happen automatically
        } else if ( id == R.id.action_refresh) {
            updateMovies();
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMovies() {
        CinefileSyncAdapter.syncImmediately(this);
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        Log.v(LOG_TAG, "onItemSelected called");
        if (mTwoPane) {
            Log.v(LOG_TAG, "In a 2-pane layout");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(MovieDetailActivityFragment.DETAIL_URI, contentUri);

            MovieDetailActivityFragment fragment = new MovieDetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, DETAIL_FRAGMENT_TAG)
                    .commit();
        } else {
            Log.v(LOG_TAG, "In a single pane layout");
            Intent intent = new Intent(this, MovieDetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "In onResume");
        String apiSortOrder = Utility.getAPISortOrder(this);
        // update the sort order in the grid fragment if it changed
        if (apiSortOrder != null && !apiSortOrder.equals(mApiSortOrder)) {
            PosterGridFragment ff = (PosterGridFragment)getSupportFragmentManager().
                    findFragmentByTag(POSTER_GRID_FRAGMENT_TAG);
            if ( null != ff ) {
                ff.onSortChanged();
            }
            mApiSortOrder = apiSortOrder;
        }
    }
}
