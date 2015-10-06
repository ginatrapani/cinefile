package org.ginatrapani.cinefile;

import android.content.Context;
import android.content.SharedPreferences;

public class FavoritesHelper {

    private final String FAVORITES_PREF_KEY = "FAVES";

    private final String FAVORITES_SIZE_PREF_KEY = "FAVE_ARRAY_SIZE";

    private final String LOG_TAG = FavoritesHelper.class.getSimpleName();

    public long[] getFavorites(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences(FAVORITES_PREF_KEY, 0);
        int size = prefs.getInt(FAVORITES_SIZE_PREF_KEY, 0);
        long[] favesArray = new long[size];
        for(int i=0; i<size; i++) {
            favesArray[i] = Long.parseLong(prefs.getString("fav_array_" + i, null));
            //Log.v(LOG_TAG, "getting fave " + favesArray[i]);
        }
        return favesArray;
    }

    public boolean isFavorite(Context mContext, long movieId) {
        long[] favesArray = getFavorites(mContext);
        //If ID is in the list, return true
        for(int i = 0; i < favesArray.length; i++) {
            //Log.v(LOG_TAG, "Checking if " + movieId + " = " + favesArray[i]);
            if (favesArray[i] == movieId) {
                //Log.v(LOG_TAG, "fave exists");
                return true;
            }
        }
        return false;
    }

    public boolean unFavorite(Context mContext, long movieId) {
        long[] favesArray = getFavorites(mContext);

        //If new fave is not already in the list, return true
        boolean isInList = false;
        for(int i = 0; i < favesArray.length; i++) {
            //Log.v(LOG_TAG, "Checking if " + newFav + " = " + favesArray[i]);
            if (favesArray[i] == movieId) {
                //Log.v(LOG_TAG, "fave exists");
                isInList = true;
            }
        }
        if (isInList) {
            //Create a new faves array and store it
            long[] newFavesArray = new long[favesArray.length-1];
            //Load existing faves into new faves array
            int j = 0;
            for(int i = 0; i < favesArray.length; i++) {
                if (favesArray[i] != movieId) {
                    newFavesArray[j] = favesArray[i];
                    j++;
                }
            }
            int size = favesArray.length - 1;

            SharedPreferences prefs = mContext.getSharedPreferences(FAVORITES_PREF_KEY, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(FAVORITES_SIZE_PREF_KEY, size);
            //Log.v(LOG_TAG, "putting total faves " + size);

            for(int i=0;i<size;i++) {
                editor.putString("fav_array_" + i, new Long(newFavesArray[i]).toString());
                //Log.v(LOG_TAG, "putting fave " + newFavesArray[i]);
            }
            return editor.commit();
        } else {
            return true;
        }
    }

    public boolean saveFavorite(Context mContext, long newFav) {
        long[] favesArray = getFavorites(mContext);

        //If new fave is already in the list, return true
        for(int i = 0; i < favesArray.length; i++) {
            //Log.v(LOG_TAG, "Checking if " + newFav + " = " + favesArray[i]);
            if (favesArray[i] == newFav) {
                //Log.v(LOG_TAG, "fave already exists");
                return true;
            }
        }

        //Create a new faves array and store it
        long[] newFavesArray = new long[favesArray.length+1];
        //Load existing faves into new faves array
        for(int i = 0; i < favesArray.length; i++) {
            newFavesArray[i] = favesArray[i];
        }
        //Add the new fave to the end of the array
        newFavesArray[favesArray.length] = newFav;
        int size = favesArray.length + 1;

        SharedPreferences prefs = mContext.getSharedPreferences(FAVORITES_PREF_KEY, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(FAVORITES_SIZE_PREF_KEY, size);
        //Log.v(LOG_TAG, "putting total faves " + size);

        for(int i=0;i<size;i++) {
            editor.putString("fav_array_" + i, new Long(newFavesArray[i]).toString());
            //Log.v(LOG_TAG, "putting fave " + newFavesArray[i]);
        }
        return editor.commit();
    }
}
