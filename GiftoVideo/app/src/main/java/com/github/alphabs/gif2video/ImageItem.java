package com.github.alphabs.gif2video;

public class ImageItem {

    private String Path;
    private boolean isChecked;

    public ImageItem(String path, boolean ischeck) {
        setPath(path);
        setChecked(ischeck);
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String value) {
        Path = value;
    }

    public boolean getChecked() {
        return isChecked;
    }

    public void setChecked(boolean value) {
        isChecked = value;
    }
}
