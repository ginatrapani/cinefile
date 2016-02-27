package org.ginatrapani.cinefile.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.ginatrapani.cinefile.R;
import org.ginatrapani.cinefile.Utility;
import org.ginatrapani.cinefile.data.MovieContract;
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
 * Created by ginatrapani on 2/27/16.
 */

public class CinefileSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = CinefileSyncAdapter.class.getSimpleName();

    public CinefileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        String sortOrder = Utility.getAPISortOrder(getContext());
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            // Construct the URL for the TheMovieDB query
            final String MOVIE_BASE_URL =
                    "http://api.themoviedb.org/3/discover/movie?";
            final String SORT_BY_PARAM = "sort_by";
            final String API_KEY_PARAM = "api_key";

            String api_key = getContext().getString(R.string.api_key);

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

            movieValues.put(MovieContract.MovieEntry.COLUMN_TMDB_ID,
                    singleMovie.getString(TMDB_MOVIE_ID));
            movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH,
                    singleMovie.getString(TMDB_POSTER_PATH));
            movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE,
                    singleMovie.getString(TMDB_VOTE_AVG));
            movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW,
                    singleMovie.getString(TMDB_OVERVIEW));
            movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE,
                    singleMovie.getString(TMDB_TITLE));
            movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
                    singleMovie.getString(TMDB_RELEASE));
            movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY,
                    singleMovie.getString(TMDB_POPULARITY));

            cVVector.add(movieValues);
        }

        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            getContext().getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, cvArray);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
                if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                    return null;
                }
                /*
                 * If you don't set android:syncable="true" in
                 * in your <provider> element in the manifest,
                 * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
                 * here.
                 */
        }
        return newAccount;
    }
}