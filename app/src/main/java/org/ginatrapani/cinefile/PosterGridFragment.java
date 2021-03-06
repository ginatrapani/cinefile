package org.ginatrapani.cinefile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        if(savedInstanceState != null) {
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
            new FetchMoviesTask().execute(sortOrder);
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

    public class FetchMoviesTask extends AsyncTask<String, Void, Movie[]> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected Movie[] doInBackground(String... params) {
            String sortOrder = params[0];
            if (sortOrder.equals("favorites")) {
                FavoritesHelper favHelper = new FavoritesHelper();
                long[] favesArray = favHelper.getFavorites(getActivity());
                Movie[] moviesToShow = new Movie[favesArray.length];
                int i;
                for(i=0; i < favesArray.length; i++) {
                    moviesToShow[i] = getMovieFromAPI(favesArray[i]);
                }
                return moviesToShow;
            } else {
                return getMoviesFromAPI(sortOrder);
            }
        }

        private Movie[] getMoviesFromAPI(String sortOrder) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Raw JSON response as a string
            String moviesJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB query
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_BY_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";
                String api_key = getString(R.string.api_key);

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_BY_PARAM, sortOrder)
                        .appendQueryParameter(API_KEY_PARAM, api_key)
                        .build();

                URL url = new URL(builtUri.toString());

                //Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to TheMovieDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
                //Log.v(LOG_TAG, "Movies JSON " + moviesJsonStr);
                try {
                    return getMoviesFromJsonStr(moviesJsonStr);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error ", e);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        private Movie getMovieFromAPI(long movieId) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Raw JSON response as a string
            String moviesJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB query
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/" + new Long(movieId).toString() + "?";
                final String API_KEY_PARAM = "api_key";
                String api_key = getString(R.string.api_key);

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, api_key)
                        .build();

                URL url = new URL(builtUri.toString());

                //Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to TheMovieDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
                //Log.v(LOG_TAG, "Movies JSON " + moviesJsonStr);
                try {
                    JSONObject moviesJson = new JSONObject(moviesJsonStr);
                    return getMovieFromJSONObject(moviesJson);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error ", e);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        /**
         * Take the String representing the complete list of movies in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private Movie[] getMoviesFromJsonStr(String moviesJsonStr)
            throws JSONException {
            final String TMDB_RESULTS = "results";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);

            Movie[] resultMovies = new Movie[moviesArray.length()];
            for(int i = 0; i < moviesArray.length(); i++) {
                // Get the JSON object representing a movie
                JSONObject singleMovie = moviesArray.getJSONObject(i);
                resultMovies[i] = getMovieFromJSONObject(singleMovie);
            }
            return resultMovies;
        }

        private Movie getMovieFromJSONObject(JSONObject movieJsonObj) throws JSONException {
            final String TMDB_POSTER_PATH = "poster_path";
            final String TMDB_MOVIE_ID = "id";
            final String TMDB_OVERVIEW = "overview";
            final String TMDB_TITLE = "original_title";
            final String TMDB_RELEASE = "release_date";
            final String TMDB_VOTE_AVG = "vote_average";

            //Log.v(LOG_TAG, "Single Movie JSON Object " + movieJsonObj.toString());
            String posterPath = movieJsonObj.getString(TMDB_POSTER_PATH);
            long movieId = movieJsonObj.getLong(TMDB_MOVIE_ID);
            String overview = movieJsonObj.getString(TMDB_OVERVIEW);
            String title = movieJsonObj.getString(TMDB_TITLE);
            String releaseDate = movieJsonObj.getString(TMDB_RELEASE);
            String voteAverage = movieJsonObj.getString(TMDB_VOTE_AVG);

            return new Movie(movieId, posterPath, overview, title, releaseDate,
                    voteAverage);
        }

        @Override
        protected void onPostExecute(Movie[] result) {
            if (result != null) {
                mMoviesAdapter.clear();
                for (Movie singleMovie : result) {
                    //Log.v(LOG_TAG, "Adding " + singleMovie + " to adapter");
                    mMoviesAdapter.add(singleMovie);
                }
                if (result[0] != null) {
                    ((MovieDetailActivityFragment.Callback) getActivity()).onMovieListLoaded(result[0]);
                }
                mMoviesAdapter.notifyDataSetChanged();
            }
        }
    }
}

class ImageAdapter extends BaseAdapter {
    private Context mContext;
    // references to our movies
    private ArrayList<Movie> mMovies = new ArrayList();

    public void clear() {
        mMovies.clear();
    }

    public void add(Movie movie) {
        mMovies.add(movie);
    }

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return mMovies.size();
    }

    public Object getItem(int position) {
        return mMovies.get(position);
    }

    public long getItemId(int position) {
        return mMovies.get(position).getId();
    }

    public ArrayList<Movie> getMovies() {
        return mMovies;
    }

    public void setMovies(ArrayList<Movie> mMovies) {
        this.mMovies = mMovies;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(0, 0, 0, 0);
        } else {
            imageView = (ImageView) convertView;
        }
        Movie movie = mMovies.get(position);
        Picasso.with(mContext).load(movie.getPosterPath())
                .resize(movie.getDefaultWidth() * 2, movie.getDefaultHeight() * 2)
                .into(imageView);
        return imageView;
    }
}

class Movie implements Parcelable {

    private long movieId;

    private static final String KEY_MOVIE_ID = "movie_id";

    private String posterPath;

    private static final String KEY_POSTER_PATH = "poster_path";

    private String overview;

    private static final String KEY_OVERVIEW = "overview";

    private String title;

    private static final String KEY_TITLE = "title";

    private String releaseDate;

    private static final String KEY_RELEASE_DATE = "release_date";

    private String voteAverage;

    private static final String KEY_VOTE_AVERAGE = "vote_average";

    private final String posterDomainPath = "http://image.tmdb.org/t/p/w185";
    public final int defaultWidth = 185;
    public final int defaultHeight = 278;

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator CREATOR =
        new Parcelable.Creator() {
            public Movie createFromParcel(Parcel source) {
                // read the bundle containing key value pairs from the parcel
                Bundle bundle = source.readBundle();

                // instantiate a person using values from the bundle
                return new Movie( bundle.getLong(KEY_MOVIE_ID), bundle.getString(KEY_POSTER_PATH),
                        bundle.getString(KEY_OVERVIEW), bundle.getString(KEY_TITLE),
                        bundle.getString(KEY_RELEASE_DATE),
                        bundle.getString(KEY_VOTE_AVERAGE));
            }

            public Movie[] newArray(int size) {
                return new Movie[size];
            }
        };


    public Movie(long movieId, String posterPath, String overview, String title, String releaseDate,
                 String voteAverage) {
        this.movieId = movieId;
        this.posterPath = this.posterDomainPath + posterPath;
        this.overview = overview;
        this.title = title;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
    }

    public String getPosterPath() {
        return this.posterPath;
    }

    public long getId() {
        return this.movieId;
    }

    public String getOverview() {
        return this.overview;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getVoteAverage() {
        return voteAverage;
    }

    public int getDefaultWidth() {
        return defaultWidth;
    }

    public int getDefaultHeight() {
        return defaultHeight;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // create a bundle for the key value pairs
        Bundle bundle = new Bundle();

        // insert the key value pairs to the bundle
        bundle.putLong(KEY_MOVIE_ID, movieId);
        bundle.putString(KEY_POSTER_PATH, posterPath);
        bundle.putString(KEY_OVERVIEW, overview);
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_RELEASE_DATE, releaseDate);
        bundle.putString(KEY_VOTE_AVERAGE, voteAverage);
        // write the key value pairs to the parcel
        dest.writeBundle(bundle);
    }
}