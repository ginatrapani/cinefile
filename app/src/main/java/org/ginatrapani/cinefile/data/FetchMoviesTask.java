package org.ginatrapani.cinefile.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.ginatrapani.cinefile.FavoritesHelper;
import org.ginatrapani.cinefile.MovieDetailActivityFragment;
import org.ginatrapani.cinefile.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

            String api_key = mContext.getString(R.string.api_key);

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
            String api_key = mContext.getString(R.string.api_key);

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
                ((MovieDetailActivityFragment.Callback) mContext).onMovieListLoaded(result[0]);
            }

            mMoviesAdapter.notifyDataSetChanged();
        }
    }
}
