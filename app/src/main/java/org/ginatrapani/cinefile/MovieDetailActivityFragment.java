package org.ginatrapani.cinefile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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

    private TrailerAdapter mTrailerAdapter;

    private ReviewAdapter mReviewAdapter;

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mTrailerAdapter = new TrailerAdapter(this.getActivity());

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_trailer);
        listView.setAdapter(mTrailerAdapter);

        mReviewAdapter = new ReviewAdapter(this.getActivity());

        // Get a reference to the ListView, and attach this adapter to it.
        ListView reviewListView = (ListView) rootView.findViewById(R.id.listview_review);
        reviewListView.setAdapter(mReviewAdapter);

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

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    Trailer trailer = mTrailerAdapter.getItem(position);
                    Uri builtUri = Uri.parse("http://youtube.com//watch?v="+trailer.getKey()).
                            buildUpon().build();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(builtUri);
                    startActivity(intent);
                }
            });

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
            updateReviews(movie.getId());
        }
    }

    private void updateTrailers(long movieId) {
        new FetchTrailersTask().execute(new Long(movieId).toString());
    }

    private void updateReviews(long movieId) {
        new FetchReviewsTask().execute(new Long(movieId).toString());
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
                    mTrailerAdapter.add(singleTrailer);
                }
                mTrailerAdapter.notifyDataSetChanged();
            }
        }
    }

    public class FetchReviewsTask extends AsyncTask<String, Void, Review[]> {

        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();

        @Override
        protected Review[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Raw JSON response as a string
            String reviewsJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB query
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/" + params[0] + "/reviews?";
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
                reviewsJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Trailers JSON " + reviewsJsonStr);
                try {
                    return getReviewDataFromJson(reviewsJsonStr);
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

        private Review[] getReviewDataFromJson(String reviewsJsonStr)
                throws JSONException {
            final String TMDB_RESULTS = "results";
            final String TMDB_REVIEW_CONTENT = "content";
            final String TMDB_REVIEW_AUTHOR = "author";

            JSONObject reviewsJson = new JSONObject(reviewsJsonStr);
            JSONArray reviewsArray = reviewsJson.getJSONArray(TMDB_RESULTS);

            Review[] resultReviews = new Review[reviewsArray.length()];
            for(int i = 0; i < reviewsArray.length(); i++) {
                // Get the JSON object representing a review
                JSONObject singleReview = reviewsArray.getJSONObject(i);
                Log.v(LOG_TAG, "Single Review JSON Object " + singleReview.toString());
                String reviewContent = singleReview.getString(TMDB_REVIEW_CONTENT);
                String reviewAuthor = singleReview.getString(TMDB_REVIEW_AUTHOR);
                resultReviews[i] = new Review(reviewAuthor, reviewContent);
            }
            return resultReviews;
        }

        @Override
        protected void onPostExecute(Review[] result) {
            if (result != null) {
                mReviewAdapter.clear();
                for(Review singleReview : result) {
                    Log.v(LOG_TAG, "Adding " + singleReview.getContent() + " to adapter");
                    mReviewAdapter.add(singleReview);
                }
                mReviewAdapter.notifyDataSetChanged();
            }
        }
    }
}

class TrailerAdapter extends BaseAdapter {
    // references to our trailers
    private ArrayList<Trailer> mTrailers = new ArrayList();

    private LayoutInflater inflater = null;

    public void clear() {
        mTrailers.clear();
    }

    public void add(Trailer trailer) {
        mTrailers.add(trailer);
    }

    public TrailerAdapter(Context c) {
        inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return mTrailers.size();
    }

    public Trailer getItem(int position) {
        return mTrailers.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if(convertView == null)
            vi = inflater.inflate(R.layout.list_item_trailer, null);
        TextView name = (TextView)vi.findViewById(R.id.list_item_trailer_textview);
        name.setText(this.getItem(position).getName());
        return vi;
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

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}

class ReviewAdapter extends BaseAdapter {
    // references to our reviews
    private ArrayList<Review> mReviews = new ArrayList();

    private LayoutInflater inflater = null;

    public void clear() {
        mReviews.clear();
    }

    public void add(Review review) {
        mReviews.add(review);
    }

    public ReviewAdapter(Context c) {
        inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return mReviews.size();
    }

    public Review getItem(int position) {
        return mReviews.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if(convertView == null)
            vi = inflater.inflate(R.layout.list_item_review, null);

        TextView name = (TextView)vi.findViewById(R.id.list_item_review_content_textview);
        name.setText(this.getItem(position).getContent());

        TextView author = (TextView)vi.findViewById(R.id.list_item_review_author_textview);
        author.setText(this.getItem(position).getAuthor());

        return vi;
    }
}

class Review {
    String author;
    String content;

    public Review(String author, String content) {
        this.author = author;
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }
}


