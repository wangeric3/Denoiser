package com.wangeric3.denoiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

public class Recorder extends AppCompatActivity {
    public static final int RequestPermissionCode = 1;
    private static final String EXTRA_RECORDED_FILE = "com.wangeric3.android.ModelTest.recorded_file";

    Button buttonStart, buttonStop, buttonSave;
    TextView fileName;
    Chronometer recordTime;
    File saveDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Denoiser");
    File originalDir = new File(saveDir.getAbsolutePath() + "/original");
    File cleanDir = new File(saveDir.getAbsolutePath() + "/clean");
    String audioFilePath = null;
    private RecordWaveTask recordTask = null;
    Calendar cal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        if (!saveDir.exists()) saveDir.mkdirs();
        if (!originalDir.exists()) originalDir.mkdirs();
        if (!cleanDir.exists()) cleanDir.mkdirs();
        initViews();

        recordTask = (RecordWaveTask) getLastCustomNonConfigurationInstance();
        if (recordTask == null) {
            recordTask = new RecordWaveTask(this);
        } else {
            recordTask.setContext(this);
        }

    }

    private void initViews(){
        buttonStart = (Button) findViewById(R.id.start_recording);
        buttonStop = (Button) findViewById(R.id.stop_recording);
        buttonSave = (Button) findViewById(R.id.save_recording);

        fileName = (TextView) findViewById(R.id.file_name);
        recordTime = (Chronometer) findViewById(R.id.record_time);

        buttonStop.setEnabled(false);
        buttonStart.setEnabled(true);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioFilePath == null) {
                    audioFilePath = saveDir.getAbsolutePath() + "/original/" + CreateAudioFileName(cal.getTime());
                    String[] split = audioFilePath.split("/");
                    fileName.setText(split[split.length-1]);
                }

                try {
                    launchTask();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                recordTime.setBase(SystemClock.elapsedRealtime());
                recordTime.start();
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
                buttonSave.setEnabled(false);

            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordTask.isCancelled() && recordTask.getStatus() == AsyncTask.Status.RUNNING) {
                    recordTask.cancel(false);

                    recordTime.stop();
                    buttonStop.setEnabled(false);
                    buttonStart.setEnabled(true);
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Save");
                } else {
                    Toast.makeText(Recorder.this, "Task not running.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioFilePath == null){
                    Intent data = new Intent();
                    setResult(Activity.RESULT_CANCELED,data);
                    finish();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Recorder.this);
                    builder.setTitle("Edit File Name");
                    final EditText input = new EditText(Recorder.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    String[] split = audioFilePath.split("/");
                    final String prevName = split[split.length-1];
                    input.setText(prevName);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String newName = input.getText().toString();
                            if (newName.substring(newName.length()-4,newName.length()).equals(".wav")){
                                audioFilePath = originalDir.getAbsolutePath() + "/" + newName;
                                fileName.setText(newName);
                            } else {
                                audioFilePath = originalDir.getAbsolutePath() + "/" + newName + ".wav";
                                fileName.setText(newName + ".wav");
                            }
                            File f = new File(originalDir.getAbsolutePath() + "/" + prevName);
                            File fTemp = new File(audioFilePath);
                            f.renameTo(fTemp);
                            Intent data = new Intent();
                            data.putExtra(EXTRA_RECORDED_FILE, audioFilePath);
                            setResult(Activity.RESULT_OK,data);
                            finish();

                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
            }
        });
    }

    private BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                // Start recording audio
            }
        }
    };

    public String CreateAudioFileName(Date date) {
        StringBuilder sb = new StringBuilder();
        String[] dateSplit = date.toString().split(" ");
        sb.append(dateSplit[1]);
        for (int i = 2; i < 4; i++) sb.append("-").append(dateSplit[i]);
        return sb.append(".wav").toString();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        recordTask.setContext(null);
        return recordTask;
    }

    private void launchTask() {
        switch (recordTask.getStatus()) {
            case RUNNING:
                Toast.makeText(this, "Task already running...", Toast.LENGTH_SHORT).show();
                return;
            case FINISHED:
                recordTask = new RecordWaveTask(this);
                break;
            case PENDING:
                if (recordTask.isCancelled()) {
                    recordTask = new RecordWaveTask(this);
                }
        }
        File wavFile = new File(audioFilePath);
        recordTask.execute(wavFile);
    }

    public static String decodeIntent(Intent result){
        return result.getStringExtra(EXTRA_RECORDED_FILE);
    }

    public static boolean hasThisExtra(Intent data){
        return data.hasExtra(EXTRA_RECORDED_FILE);
    }


}
