package com.github.alphabs.gif2video;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
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

    private static final int IDLE = 0;
    private static final int RUNNING = 1;
    private static final int STOPPING = 2;

    private boolean serviceBound = false;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            fBindMessenger = new Messenger(service);
            fBindMessenger = new Messenger(service);

            try {
                Message msg = Message.obtain(null, FFmpegService.MSG_REGISTER_CLIENT);
                msg.replyTo = fMessenger;
                fBindMessenger.send(msg);
            }
            catch (RemoteException e) {
                Log.i("failed to set replyTo" , e.toString());
            }

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fBindMessenger = null;
            serviceBound = false;
        }
    };

    private Messenger fBindMessenger;
    private Messenger fMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("handleMessage", Integer.toString(msg.what));
            switch (msg.what) {
                case FFmpegService.MSG_STATE:
                    Log.d("MSG_STATE", Integer.toString(msg.arg1));
                    setState(msg.arg1);
                    break;
                case FFmpegService.MSG_PROGRESS:
                    setServiceProgress(msg.arg1, msg.arg2);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

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

        doBind();
    }

    private void doBind() {
        Intent intent = new Intent(this, FFmpegService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbind() {
        if (serviceBound) {
            if (fBindMessenger != null) {
                try {
                    Message msg = Message.obtain(null, FFmpegService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = fMessenger;
                    fBindMessenger.send(msg);
                }
                catch (RemoteException e) {
                    Log.i("Failed to doUnbind", e.toString());
                }
            }

            unbindService(connection);
            serviceBound = false;
        }
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

        doBind();
    }

    private void onBtnStopClicked(View v) {
        if (serviceBound) {
            try {
                fBindMessenger.send(Message.obtain(null, FFmpegService.MSG_STOP));
            } catch (RemoteException e) {
                Log.e("remote exception", e.toString());
            }
        }
        else {
            Intent intent = new Intent(this, FFmpegService.class);
            intent.setAction(FFmpegService.STOP_ACTION);
            startService(intent);
        }
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

    private void setState(int state) {
        if (state == IDLE) {
            binding.tProgress.setText("IDLE");
            binding.btnStart.setEnabled(true);
            binding.btnTestStart.setEnabled(true);
            binding.btnStop.setEnabled(false);
            setServiceProgress(0,0);

            doUnbind();
        }
        else if (state == STOPPING) {
            binding.tProgress.setText("STOPPING");
            binding.btnStart.setEnabled(false);
            binding.btnTestStart.setEnabled(false);
            binding.btnStop.setEnabled(false);
        }
        else if (state == RUNNING) {
            binding.tProgress.setText("RUNNING");
            binding.btnStart.setEnabled(false);
            binding.btnTestStart.setEnabled(false);
            binding.btnStop.setEnabled(true);
        }
    }

    private void setServiceProgress(int value, int total) {
        binding.progressbar.setMax(total);
        binding.progressbar.setProgress(value);
        binding.tTextView.setText(value + " / " + total);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbind();
    }

    private void checkPermissions() {
        String[] perms = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permissions Pass", Toast.LENGTH_SHORT).show();
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
