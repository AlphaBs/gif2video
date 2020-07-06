package com.github.alphabs.gif2video;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;

public class ImageItemView extends RelativeLayout {
    final int THUMBNAIL_SIZE = 64;

    ImageView imgThumbnail;
    CheckBox cbCheck;

    public ImageItemView(Context context) {
        super(context);
        init(context);
    }

    public ImageItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.image_item, this, true);

        imgThumbnail = (ImageView) findViewById(R.id.imgThumbnail);
        cbCheck = (CheckBox) findViewById(R.id.cbCheck);
    }

    public Drawable getThumbnail() {
        return imgThumbnail.getDrawable();
    }

    public void setThumbnail(Drawable drawable) {
        imgThumbnail.setImageDrawable(drawable);
    }

    public void setThumbnail(String path) {
        Glide.with(this)
                .load(path)
                .thumbnail(0.25f)
                .into(imgThumbnail);
    }

    public boolean getChecked() {
        return cbCheck.isChecked();
    }

    public void setChecked(boolean value) {
        cbCheck.setChecked(value);
    }
}
