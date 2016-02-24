package org.ginatrapani.cinefile.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.ginatrapani.cinefile.R;

/**
 * Created by ginatrapani on 2/22/16.
 */
public class ImageAdapter extends CursorAdapter {

    public ImageAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item_poster, parent, false);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int idx_poster = cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_PATH);
        String posterPath = cursor.getString(idx_poster);

        ImageView imageView = (ImageView)view;
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(0, 0, 0, 0);
        Picasso.with(context).load(Movie.POSTER_DOMAIN_PATH + posterPath)
                .resize(Movie.DEFAULT_WIDTH * 2, Movie.DEFAULT_HEIGHT * 2)
                .into(imageView);
    }
}
