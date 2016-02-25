package org.ginatrapani.cinefile;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.ginatrapani.cinefile.data.MovieContract.MovieEntry;

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
        Fill in the view with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ImageView imageView = (ImageView)view;
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(0, 0, 0, 0);
        Picasso.with(context).load(
                MovieEntry.POSTER_DOMAIN_PATH + cursor.getString(PosterGridFragment.COL_POSTER_PATH
                )).resize(MovieEntry.DEFAULT_WIDTH * 2, MovieEntry.DEFAULT_HEIGHT * 2)
                .into(imageView);
    }
}
