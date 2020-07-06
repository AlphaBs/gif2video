package com.github.alphabs.gif2video;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    Handler handler;
    ImageAdapter imgAdapter;
    TextView progressText;
    ProgressBar progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        progressText = (TextView) findViewById(R.id.progressText);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);

        String[] perms = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permissions Pass", Toast.LENGTH_LONG).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "accept plz", 31, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void onButtonTestClicked(View v) {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        properties.show_hidden_files = true;

        FilePickerDialog dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select Directory");
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                Log.i("FilePicker", files[0]);
                loadFiles(files[0]);
            }
        });
        dialog.show();
    }

    void loadFiles(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File dir = new File(path);
                File[] files = dir.listFiles();

                final int fileLength = files.length;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressbar.setMax(fileLength);
                    }
                });

                for (int i = 0; i < files.length; i++) {
                    try {
                        File item = files[i];
                        long lastMod = item.lastModified();

                        String basePath = item.getParent();
                        String fileName = item.getName();

                        Log.i("ffmpeg excute", fileName);
                        String name = fileName.substring(0, fileName.lastIndexOf("."));
                        String ext = fileName.substring(fileName.lastIndexOf("."));

                        if (!ext.equals("gif"))
                            continue;

                        String inPath = basePath + "/" + fileName;
                        String outTempPath = basePath + "/" + name + ".temp.mp4";
                        String outPath = basePath + "/" + name + ".mp4";
                        File outFile = new File(outPath);

                        if (outFile.exists())
                            continue;

                        File outTempFile = new File(outTempPath);
                        if (outTempFile.exists()) {
                            outTempFile.delete();
                        }

                        Log.i("ffmpeg excute", inPath + " + " + outTempPath);
                        int rc = FFmpeg.execute(getCommand(inPath, outTempPath));
                        if (rc != 0) {
                            Log.i("ffmpeg", Integer.toString(rc));
                            Config.printLastCommandOutput(Log.INFO);
                            continue;
                        }

                        outTempFile.renameTo(outFile);
                        outFile.setLastModified(lastMod);

                        final int progressValue = i + 1;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressbar.setProgress(progressValue);
                                progressText.setText(progressValue + " / " + fileLength);
                            }
                        });
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "done", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    String getCommand(String input, String output) {
        return "-i \""+input+"\" -c:v libx264 -movflags faststart -vf \"pad=ceil(iw/2)*2:ceil(ih/2)*2\" -pix_fmt yuv420p \"" + output + "\"";
    }

    class ImageAdapter extends BaseAdapter {
        ArrayList<ImageItem> items = new ArrayList<ImageItem>();

        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(ImageItem item) {
            items.add(item);
        }

        public ImageItem getItem(int pos) {
            return items.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup viewGroup) {
            ImageItemView view = new ImageItemView(getApplicationContext());
            ImageItem item = items.get(pos);
            view.setThumbnail(item.getPath());
            view.setChecked(item.getChecked());
            return view;
        }
    }
}
