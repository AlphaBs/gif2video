package com.github.alphabs.gif2video;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.github.alphabs.gif2video.databinding.ActivityMainBinding;

import java.io.File;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private Handler handler;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        handler = new Handler();

        String[] presetNames = new String[] {
                "libx264 default (권장)",
                "libvpx"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, presetNames);
        binding.spPreset.setAdapter(adapter);

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

        binding.spPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSpPresetItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        checkPermissions();
    }

    private void onBtnSelectPathClicked(View v) {
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
                if (files != null && files.length > 0)
                    binding.txtPath.setText(files[0]);
            }
        });
        dialog.show();
    }

    private void onBtnStartClicked(View v) {
        FFmpegServiceOption option = new FFmpegServiceOption();
        option.setTargetDir(binding.txtPath.getText().toString());
        option.setFfmpegArguments(binding.txtArg.getText().toString());
        option.setOutputExtension(binding.txtExt.getText().toString());
        option.setTargetExtension(binding.txtInputExt.getText().toString());
        option.setPreserveFileDate(binding.cbPreserveDate.isChecked());
        option.setRemoveCompletedFile(binding.cbRemoveCompleted.isChecked());

        Intent intent = new Intent(this, FFmpegService.class);
        intent.setAction(FFmpegService.START_ACTION);
        intent.putExtra("option", option);
        startForegroundService(intent);

        //setStartUIEnable(false);
    }

    private void onBtnStopClicked(View v) {
        Intent intent = new Intent(this, FFmpegService.class);
        intent.setAction(FFmpegService.STOP_ACTION);
        startService(intent);

        //handler.postDelayed(new Runnable() {
        //    @Override
        //    public void run() {
        //        setStartUIEnable(true);
        //    }
        //}, 2000);
    }

    private void onBtnTestStartClicked(View v) {

    }

    private void onBtnHelpClicked(View v) {

    }

    private void onSpPresetItemSelected(int pos) {
        String ext = "";
        String arg = "";

        switch (pos) {
            case 0:
                ext = "mp4";
                arg = "-i {input} -c:v libx264 -movflags faststart -f {outext} -vf \"pad=ceil(iw/2)*2:ceil(ih/2)*2\" -pix_fmt yuv420p {output}";
                break;
            case 1:
                ext = "mp4";
                arg = "-i {input} -c:v libvpx -b:v 3M -auto-alt-ref 0 -f {outext} {output}";
                break;
        }

        binding.txtArg.setText(arg);
        binding.txtExt.setText(ext);
    }

    private void setStartUIEnable(boolean value) {
        binding.btnStart.setEnabled(value);
        binding.btnTestStart.setEnabled(value);
        binding.btnStop.setEnabled(!value);
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
}
