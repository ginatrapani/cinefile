package org.ginatrapani.cinefile.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ginatrapani on 2/22/16.
 */
public class Movie implements Parcelable {

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