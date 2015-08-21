package org.ginatrapani.cinefile;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.Toast;

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
        new FetchMoviesTask().execute("popularity.desc");

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText( getActivity(), "" + position,
                        Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            // @TODO use the right sort order based on user setting
            new FetchMoviesTask().execute("popularity.desc");
            return true;
        }
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

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);

            Movie[] resultMovies = new Movie[moviesArray.length()];
            for(int i = 0; i < moviesArray.length(); i++) {
                // Get the JSON object representing a movie
                JSONObject singleMovie = moviesArray.getJSONObject(i);
                Log.v(LOG_TAG, "Single Movie JSON Object " + singleMovie.toString());
                String posterPath = singleMovie.getString(TMDB_POSTER_PATH);
                long movieId = singleMovie.getLong(TMDB_MOVIE_ID);
                resultMovies[i] = new Movie(movieId, posterPath);
            }
            return resultMovies;
        }

        @Override
        protected void onPostExecute(Movie[] result) {
            if (result != null) {
                mMoviesAdapter.clear();
                for(Movie singleMovie : result) {
                    Log.v(LOG_TAG, "Adding " + singleMovie + " to adapter");
                    mMoviesAdapter.add(singleMovie);
                }
                mMoviesAdapter.notifyDataSetChanged();
            }
        }
    }
}

class ImageAdapter extends BaseAdapter {
    private Context mContext;

    public void clear() {
        mThumbIds.clear();
    }

    public void add(Movie movie) {
        mThumbIds.add(movie);
    }

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return mThumbIds.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
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

        Picasso.with(mContext).load(mThumbIds.get(position).getPosterPath()).into(imageView);
        return imageView;
    }

    // references to our images
    private ArrayList<Movie> mThumbIds = new ArrayList();
}

class Movie {

    private long movieId;

    private String posterPath;

    private final String posterDomainPath = "http://image.tmdb.org/t/p/w185";

    // @TODO Set the rest of the required attributes


    public Movie(long movieId, String posterPath) {
        this.movieId = movieId;
        this.posterPath = this.posterDomainPath + posterPath;
    }

    public String getPosterPath() {
        return this.posterPath;
    }
}