package org.ginatrapani.cinefile;

import android.content.Context;
import android.content.Intent;
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
        View rootView = inflater.inflate(R.layout.fragment_poster_grid, container, false);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        mMoviesAdapter = new ImageAdapter(this.getActivity());
        gridview.setAdapter(mMoviesAdapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Movie clickedMovie = (Movie) mMoviesAdapter.getItem(position);

                Intent viewMovieDetailsIntent = new Intent(getActivity(), MovieDetailActivity.class)
                        .putExtra("movie", clickedMovie);
                startActivity(viewMovieDetailsIntent);
            }
        });
        return rootView;
    }

    private void updateMovies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = prefs.getString(getString(R.string.pref_key_sort),
                getString(R.string.pref_default_sort));
        new FetchMoviesTask().execute(sortOrder);
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

    public class FetchMoviesTask extends AsyncTask<String, Void, Movie[]> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected Movie[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Raw JSON response as a string
            String moviesJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB query
                final String FORECAST_BASE_URL =
                        "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_BY_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";
                String api_key = getString(R.string.api_key);

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_BY_PARAM, params[0])
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
                    return getMovieDataFromJson(moviesJsonStr);
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
        private Movie[] getMovieDataFromJson(String moviesJsonStr)
            throws JSONException {
            final String TMDB_RESULTS = "results";
            final String TMDB_POSTER_PATH = "poster_path";
            final String TMDB_MOVIE_ID = "id";
            final String TMDB_OVERVIEW = "overview";
            final String TMDB_TITLE = "original_title";
            final String TMDB_RELEASE = "release_date";
            final String TMDB_VOTE_AVG = "vote_average";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);

            Movie[] resultMovies = new Movie[moviesArray.length()];
            for(int i = 0; i < moviesArray.length(); i++) {
                // Get the JSON object representing a movie
                JSONObject singleMovie = moviesArray.getJSONObject(i);
                //Log.v(LOG_TAG, "Single Movie JSON Object " + singleMovie.toString());
                String posterPath = singleMovie.getString(TMDB_POSTER_PATH);
                long movieId = singleMovie.getLong(TMDB_MOVIE_ID);
                String overview = singleMovie.getString(TMDB_OVERVIEW);
                String title = singleMovie.getString(TMDB_TITLE);
                String releaseDate = singleMovie.getString(TMDB_RELEASE);
                String voteAverage = singleMovie.getString(TMDB_VOTE_AVG);
                resultMovies[i] = new Movie(movieId, posterPath, overview, title, releaseDate,
                        voteAverage);
            }
            return resultMovies;
        }

        @Override
        protected void onPostExecute(Movie[] result) {
            if (result != null) {
                mMoviesAdapter.clear();
                for(Movie singleMovie : result) {
                    //Log.v(LOG_TAG, "Adding " + singleMovie + " to adapter");
                    mMoviesAdapter.add(singleMovie);
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

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
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

    private String posterPath;

    private String overview;

    private String title;

    private String releaseDate;

    private String voteAverage;

    private final String posterDomainPath = "http://image.tmdb.org/t/p/w185";
    public final int defaultWidth = 185;
    public final int defaultHeight = 278;

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator CREATOR =
        new Parcelable.Creator() {
            public Movie createFromParcel(Parcel in) {
                return new Movie(in);
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

    /**
     * Constructor to use when re-constructing object
     * from a parcel
     *
     * @param in a parcel from which to read this object
     */
    public Movie(Parcel in) {
        readFromParcel(in);
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
        // Write each field into the parcel.
        // When reading from parcel, they will come back in the same order.
        dest.writeLong(movieId);
        dest.writeString(posterPath);
        dest.writeString(overview);
        dest.writeString(title);
        dest.writeString(releaseDate);
        dest.writeString(voteAverage);
    }

    /**
     * Called from the constructor to create this
     * object from a parcel.
     *
     * @param in parcel from which to re-create object
     */
    private void readFromParcel(Parcel in) {
        // Read back each field in the order that it was written to the parcel
        movieId = in.readLong();
        posterPath = in.readString();
        overview = in.readString();
        title = in.readString();
        releaseDate = in.readString();
        voteAverage = in.readString();
    }
}