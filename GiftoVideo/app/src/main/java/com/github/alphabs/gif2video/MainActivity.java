package com.github.alphabs.gif2video;

import android.Manifest;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.github.alphabs.gif2video.databinding.ActivityMainBinding;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    Handler handler;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        handler = new Handler();

        binding.btnSelectPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnSelectPathClicked(v);
            }
        });

        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnStartClicked(v);
            }
        });

        binding.btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnStopClicked(v);
            }
        });

        binding.btnTestStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnTestStartClicked(v);
            }
        });

        binding.btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnHelpClicked(v);
            }
        });

        checkPermissions();
    }

    private void onBtnSelectPathClicked(View v) {

    }

    private void onBtnStartClicked(View v) {
        onButtonTestClicked(v);
    }

    private void onBtnStopClicked(View v) {

    }

    private void onBtnTestStartClicked(View v) {

    }

    private void onBtnHelpClicked(View v) {

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
        final Context tContext = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File dir = new File(path);
                File[] files = dir.listFiles();

                final int fileLength = files.length;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.progressbar.setMax(fileLength);
                    }
                });

                for (int i = 0; i < files.length; i++) {
                    try {
                        File item = files[i];

                        BasicFileAttributes attr = Files.readAttributes(item.toPath(), BasicFileAttributes.class);
                        long createdAt = attr.creationTime().toMillis();

                        String basePath = item.getParent();
                        String fileName = item.getName();

                        String name = fileName.substring(0, fileName.lastIndexOf("."));
                        String ext = fileName.substring(fileName.lastIndexOf("."));
                        Log.i("ffmpeg excute", name + " / " + ext);

                        if (!ext.equals(".gif"))
                            continue;

                        String inPath = basePath + "/" + fileName;
                        String outTempPath = basePath + "/" + name + ".temp.mp4";
                        String outPath = basePath + "/" + name + ".mp4";
                        final File outFile = new File(outPath);

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

                        BasicFileAttributeView attributes = Files.getFileAttributeView(outFile.toPath(), BasicFileAttributeView.class);
                        FileTime time = FileTime.fromMillis(createdAt);
                        attributes.setTimes(time, time, time);

                        final int progressValue = i + 1;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MediaScannerConnection.scanFile(tContext,
                                        new String[] { outFile.getPath() }, null,
                                        new MediaScannerConnection.OnScanCompletedListener() {
                                            public void onScanCompleted(String path, Uri uri) {
                                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                                Log.i("ExternalStorage", "-> uri=" + uri);
                                            }
                                        });
                                binding.progressbar.setProgress(progressValue);
                                binding.tPath.setText(progressValue + " / " + fileLength);
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

    private void checkPermissions() {
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
