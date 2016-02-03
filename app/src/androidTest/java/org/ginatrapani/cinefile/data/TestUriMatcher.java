package org.ginatrapani.cinefile.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Created by ginatrapani on 2/22/16.
 */
public class TestUriMatcher extends AndroidTestCase {
    // content://org.ginatrapani.cinefile.app/movies/#"
    private static final Uri TEST_MOVIE = MovieContract.MovieEntry.buildMovieUri(100);
    // content://org.ginatrapani.cinefile.app/movies"
    private static final Uri TEST_MOVIES = MovieContract.MovieEntry.CONTENT_URI;

    public void testUriMatcher() {
        UriMatcher testMatcher = MovieProvider.buildUriMatcher();

        assertEquals("Error: The MOVIE uri was matched incorrectly.",
                testMatcher.match(TEST_MOVIE), MovieProvider.MOVIE);
        assertEquals("Error: The MOVIES_SORTED uri was matched incorrectly.",
                testMatcher.match(TEST_MOVIES), MovieProvider.MOVIES);
    }
}
