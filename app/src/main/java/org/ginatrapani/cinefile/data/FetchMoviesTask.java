package org.ginatrapani.cinefile.data;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.ginatrapani.cinefile.R;
import org.ginatrapani.cinefile.Utility;
import org.ginatrapani.cinefile.data.MovieContract.MovieEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by ginatrapani on 2/22/16.
 */
public class FetchMoviesTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

    private final Context mContext;

    public FetchMoviesTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        getMoviesFromAPI();
        return null;
    }

    private void getMoviesFromAPI() {
        Log.v(LOG_TAG, "Getting movies from API");
        String sortOrder = Utility.getAPISortOrder(mContext);
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            // Construct the URL for the TheMovieDB query
            final String MOVIE_BASE_URL =
                    "http://api.themoviedb.org/3/discover/movie?";
            final String SORT_BY_PARAM = "sort_by";
            final String API_KEY_PARAM = "api_key";

            String api_key = mContext.getString(R.string.api_key);

            Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                    .appendQueryParameter(SORT_BY_PARAM, sortOrder)
                    .appendQueryParameter(API_KEY_PARAM, api_key)
                    .build();

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "Built URI " + builtUri.toString());

            // Create the request to TheMovieDB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do
                return;
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
                return;
            }
            String moviesJsonStr = buffer.toString();
            Log.v(LOG_TAG, "Movies JSON " + moviesJsonStr);
            try {
                getMoviesFromJsonStr(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the movie data, there's no point in attempting
            // to parse it.
            return;
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
    }

    /**
     * Take the String representing the complete list of movies in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getMoviesFromJsonStr(String moviesJsonStr)
            throws JSONException {
        final String TMDB_RESULTS = "results";
        final String TMDB_POSTER_PATH = "poster_path";
        final String TMDB_MOVIE_ID = "id";
        final String TMDB_OVERVIEW = "overview";
        final String TMDB_TITLE = "original_title";
        final String TMDB_RELEASE = "release_date";
        final String TMDB_VOTE_AVG = "vote_average";
        final String TMDB_POPULARITY = "popularity";

        JSONObject moviesJson = new JSONObject(moviesJsonStr);
        JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);

        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());

        for(int i = 0; i < moviesArray.length(); i++) {
            // Get the JSON object representing a movie
            JSONObject singleMovie = moviesArray.getJSONObject(i);

            ContentValues movieValues = new ContentValues();

            movieValues.put(MovieEntry.COLUMN_TMDB_ID, singleMovie.getString(TMDB_MOVIE_ID));
            movieValues.put(MovieEntry.COLUMN_POSTER_PATH, singleMovie.getString(TMDB_POSTER_PATH));
            movieValues.put(MovieEntry.COLUMN_VOTE_AVERAGE, singleMovie.getString(TMDB_VOTE_AVG));
            movieValues.put(MovieEntry.COLUMN_OVERVIEW, singleMovie.getString(TMDB_OVERVIEW));
            movieValues.put(MovieEntry.COLUMN_TITLE, singleMovie.getString(TMDB_TITLE));
            movieValues.put(MovieEntry.COLUMN_RELEASE_DATE, singleMovie.getString(TMDB_RELEASE));
            movieValues.put(MovieEntry.COLUMN_POPULARITY, singleMovie.getString(TMDB_POPULARITY));

            cVVector.add(movieValues);
        }

        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, cvArray);
        }
    }
}
