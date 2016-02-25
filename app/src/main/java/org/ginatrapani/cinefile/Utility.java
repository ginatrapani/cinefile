package org.ginatrapani.cinefile;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.ginatrapani.cinefile.data.MovieContract;

/**
 * Created by ginatrapani on 2/25/16.
 */
public class Utility {

    public static String getAPISortOrder(Context context) {
        // Sort order:  vote_average.desc, popularity.desc, or favorites.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sortOrder = prefs.getString(context.getString(R.string.pref_key_sort),
                context.getString(R.string.pref_default_sort));
        return sortOrder;
    }

    public static String getDBSortOrder(Context context) {
        String apiSortOrder = getAPISortOrder(context);
        String dbSortOrder;
        if (apiSortOrder.equals("vote_average.desc")) {
            dbSortOrder = MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE;
        } else if (apiSortOrder.equals("popularity.desc")) {
            dbSortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY;
        } else if (apiSortOrder.equals("favorites")) {
            // @TODO Do the join on the future favorites table
            dbSortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY;
        } else {
            // Default to popularity
            dbSortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY;
        }
        return dbSortOrder;
    }

}
