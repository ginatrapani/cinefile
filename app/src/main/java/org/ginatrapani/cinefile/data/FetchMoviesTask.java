package org.ginatrapani.cinefile.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.ginatrapani.cinefile.FavoritesHelper;
import org.ginatrapani.cinefile.MovieDetailActivityFragment;
import org.ginatrapani.cinefile.R;
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
public class FetchMoviesTask extends AsyncTask<String, Void, Movie[]> {

    private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

    private ImageAdapter mMoviesAdapter;
    private final Context mContext;

    public FetchMoviesTask(Context context, ImageAdapter moviesAdapter) {
        mContext = context;
        mMoviesAdapter = moviesAdapter;
    }

    @Override
    protected Movie[] doInBackground(String... params) {
        String sortOrder = params[0];
        if (sortOrder.equals("favorites")) {
            FavoritesHelper favHelper = new FavoritesHelper();
            long[] favesArray = favHelper.getFavorites(mContext);
            Movie[] moviesToShow = new Movie[favesArray.length];
            int i;
            for(i=0; i < favesArray.length; i++) {
                moviesToShow[i] = getMovie(favesArray[i]);
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
            Log.v(LOG_TAG, "Movies JSON " + moviesJsonStr);
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

    private Movie getMovie(long movieId) {
        Cursor movieCursor = mContext.getContentResolver().query(
                MovieEntry.buildMovieUri(movieId),
                null,
                null,
                null,
                null
        );
        if (movieCursor.getCount() > 0) {
            movieCursor.moveToFirst();
            return new Movie(
                    movieCursor.getLong(movieCursor.getColumnIndex(MovieEntry.COLUMN_TMDB_ID)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_POSTER_PATH)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_OVERVIEW)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_TITLE)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_RELEASE_DATE)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_VOTE_AVERAGE)),
                    movieCursor.getString(movieCursor.getColumnIndex(MovieEntry.COLUMN_POPULARITY))
            );
        } else {
            return null;
        }
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

        // Sort order:  Ascending, by date.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String sortOrder = prefs.getString(mContext.getString(R.string.pref_key_sort),
                mContext.getString(R.string.pref_default_sort));

        if (sortOrder.equals("vote_average.desc")) {
            sortOrder = MovieEntry.COLUMN_VOTE_AVERAGE;
        } else if (sortOrder.equals("popularity.desc")) {
            sortOrder = MovieEntry.COLUMN_POPULARITY;
        }
        sortOrder += " DESC";
        Uri moviesUri = MovieEntry.CONTENT_URI;

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert
        Cursor cur = mContext.getContentResolver().query(moviesUri, null, null, null, sortOrder);

        cVVector = new Vector<ContentValues>(cur.getCount());
        if ( cur.moveToFirst() ) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cur, cv);
                cVVector.add(cv);
            } while (cur.moveToNext());
        }

        Log.d(LOG_TAG, "FetchMoviesTask Complete. " + cVVector.size() + " Inserted");

        Movie[] resultMovies = convertContentValuesToMovies(cVVector);
        return resultMovies;
    }

    private Movie[] convertContentValuesToMovies(Vector<ContentValues> cvv) {
        Movie[] resultMovies = new Movie[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues movieValues = cvv.elementAt(i);
            resultMovies[i] = new Movie(movieValues.getAsInteger(MovieEntry.COLUMN_TMDB_ID),
                    movieValues.getAsString(MovieEntry.COLUMN_POSTER_PATH),
                    movieValues.getAsString(MovieEntry.COLUMN_OVERVIEW),
                    movieValues.getAsString(MovieEntry.COLUMN_TITLE),
                    movieValues.getAsString(MovieEntry.COLUMN_RELEASE_DATE),
                    movieValues.getAsString(MovieEntry.COLUMN_VOTE_AVERAGE),
                    movieValues.getAsString(MovieEntry.COLUMN_POPULARITY)
                );
        }
        return resultMovies;
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
                ((MovieDetailActivityFragment.Callback) mContext).onMovieListLoaded(result[0]);
            }

            mMoviesAdapter.notifyDataSetChanged();
        }
    }
}
