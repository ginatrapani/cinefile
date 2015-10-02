package org.ginatrapani.cinefile;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MovieDetailActivityFragment extends Fragment {

    private final String LOG_TAG = MovieDetailActivityFragment.class.getSimpleName();

    private ArrayAdapter<String> mTrailerAdapter;

    private ArrayList<Trailer> mTrailers;

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTrailerAdapter =
                new ArrayAdapter<String>
                        (this.getActivity(), // The current context (this activity)
                        R.layout.list_item_trailer, // The name of the layout ID.
                        R.id.list_item_trailer_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_trailer);
        listView.setAdapter(mTrailerAdapter);

        // The detail Activity called via intent.  Inspect the intent for movie data.
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("movie")) {
            Movie movie = intent.getParcelableExtra("movie");
            Log.v(LOG_TAG, "Overview string is " + movie.getOverview());
            ((TextView) rootView.findViewById(R.id.detail_text))
                    .setText(movie.getOverview());
            ((TextView) rootView.findViewById(R.id.movie_title))
                    .setText(movie.getTitle());
            SimpleDateFormat apiReleaseDate = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat friendlyReleaseDateFormat = new SimpleDateFormat("MMMM d, yyyy");

            String reformattedReleaseDateStr = movie.getReleaseDate();
            try {
                reformattedReleaseDateStr = friendlyReleaseDateFormat
                        .format(apiReleaseDate.parse(movie.getReleaseDate()));
            } catch (ParseException e) {
                //do nothing, default to what API returned
            }
            ((TextView) rootView.findViewById(R.id.movie_release))
                    .setText(reformattedReleaseDateStr);

            String voteAverageString = movie.getVoteAverage()
                    + " " + getResources().getString(R.string.vote_average_suffix);
            ((TextView) rootView.findViewById(R.id.vote_average))
                    .setText(voteAverageString);
            ImageView imageView = (ImageView) rootView.findViewById(R.id.movie_poster);
            Picasso.with(getActivity()).load(movie.getPosterPath())
                    .into(imageView);

//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//                @Override
//                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                    String forecast = mForecastAdapter.getItem(position);
//                    Intent intent = new Intent(getActivity(), DetailActivity.class)
//                            .putExtra(Intent.EXTRA_TEXT, forecast);
//                    startActivity(intent);
//                }
//            });

        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("movie")) {
            Movie movie = intent.getParcelableExtra("movie");
            updateTrailers(movie.getId());
        }
    }

    private void updateTrailers(long movieId) {
        new FetchTrailersTask().execute(new Long(movieId).toString());
    }

    public class FetchTrailersTask extends AsyncTask<String, Void, Trailer[]> {

        private final String LOG_TAG = FetchTrailersTask.class.getSimpleName();

        @Override
        protected Trailer[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Raw JSON response as a string
            String trailersJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB query
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/"+params[0]+"/videos?";
                final String API_KEY_PARAM = "api_key";
                String api_key = getString(R.string.api_key);

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
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
                trailersJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Trailers JSON " + trailersJsonStr);
                try {
                    return getTrailerDataFromJson(trailersJsonStr);
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
         * Take the String representing the complete list of trailers in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private Trailer[] getTrailerDataFromJson(String trailersJsonStr)
                throws JSONException {
            final String TMDB_RESULTS = "results";
            final String TMDB_TRAILER_ID = "id";
            final String TMDB_TRAILER_KEY = "key";
            final String TMDB_TRAILER_NAME = "name";
            final String TMDB_TRAILER_SITE = "site";
            final String TMDB_TRAILER_TYPE = "type";

            JSONObject trailersJson = new JSONObject(trailersJsonStr);
            JSONArray trailersArray = trailersJson.getJSONArray(TMDB_RESULTS);

            Trailer[] resultTrailers = new Trailer[trailersArray.length()];
            for(int i = 0; i < trailersArray.length(); i++) {
                // Get the JSON object representing a trailer
                JSONObject singleTrailer = trailersArray.getJSONObject(i);
                Log.v(LOG_TAG, "Single Trailer JSON Object " + singleTrailer.toString());
                String trailerId = singleTrailer.getString(TMDB_TRAILER_ID);
                String key = singleTrailer.getString(TMDB_TRAILER_KEY);
                String name = singleTrailer.getString(TMDB_TRAILER_NAME);
                String site = singleTrailer.getString(TMDB_TRAILER_SITE);
                String type = singleTrailer.getString(TMDB_TRAILER_TYPE);
                resultTrailers[i] = new Trailer(trailerId, key, name, site, type);
            }
            return resultTrailers;
        }

        @Override
        protected void onPostExecute(Trailer[] result) {
            if (result != null) {
                mTrailerAdapter.clear();
                for(Trailer singleTrailer : result) {
                    Log.v(LOG_TAG, "Adding " + singleTrailer.getName() + " to adapter");
                    mTrailerAdapter.add(singleTrailer.getName());
                }
                mTrailerAdapter.notifyDataSetChanged();
            }
        }
    }
}


class Trailer {

    private String trailerId;

    private String key;

    private String name;

    private String site;

    private String type;

    public Trailer(String trailerId, String key, String name, String site, String type) {
        this.trailerId = trailerId;
        this.key = key;
        this.name = name;
        this.site = site;
        this.type = type;
    }

    public String getTrailerId() {
        return trailerId;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    public String getType() {
        return type;
    }
}

