package org.ginatrapani.cinefile.data;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by ginatrapani on 2/22/16.
 */
public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    // references to our movies
    private ArrayList<Movie> mMovies = new ArrayList();

    public void clear() {
        mMovies.clear();
    }

    public void add(Movie movie) {
        mMovies.add(movie);
    }

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return mMovies.size();
    }

    public Object getItem(int position) {
        return mMovies.get(position);
    }

    public long getItemId(int position) {
        return mMovies.get(position).getId();
    }

    public ArrayList<Movie> getMovies() {
        return mMovies;
    }

    public void setMovies(ArrayList<Movie> mMovies) {
        this.mMovies = mMovies;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(0, 0, 0, 0);
        } else {
            imageView = (ImageView) convertView;
        }
        Movie movie = mMovies.get(position);
        Picasso.with(mContext).load(movie.getPosterPath())
                .resize(movie.getDefaultWidth() * 2, movie.getDefaultHeight() * 2)
                .into(imageView);
        return imageView;
    }
}
