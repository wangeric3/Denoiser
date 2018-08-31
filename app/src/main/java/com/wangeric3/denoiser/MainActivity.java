package com.wangeric3.denoiser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private TextView originalName, cleanName, modelLabel;
    private Button runDenoiser, playOriginal, stopOriginal, buttonChangeFile, buttonNewRecording,
    switchModel, playClean, stopClean;
    private SpectrogramView originalSpectrogram, cleanSpectrogram;

    public static final int RequestPermissionCode = 1;
    private static final int REQUEST_CODE_FILE = 0;
    private static final int REQUEST_CODE_RECORD = 1;

    private TensorFlowInferenceInterface tfHelper;
    private static final String inputName = "REG_Net/Intputs/x";
    private static final String outputName = "REG_Net/DNN/Add_4";
    private String modelName;
    String[] modelNames;
    String[] outputNames = new String[]{outputName};

    File saveDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Denoiser");
    File originalDir = new File(saveDir.getAbsolutePath() + "/original");
    File cleanDir = new File(saveDir.getAbsolutePath() + "/clean");
    String audioFilePathOriginal = null;
    String audioFilePathClean = null;

    MediaPlayer originalMP, cleanMP;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        originalName = (TextView) findViewById(R.id.original_file);
        cleanName = (TextView) findViewById(R.id.clean_file);
        modelLabel = (TextView) findViewById(R.id.model_label);
        playOriginal = (Button) findViewById(R.id.play_original);
        stopOriginal = (Button) findViewById(R.id.stop_original);
        playClean = (Button) findViewById(R.id.play_clean);
        stopClean = (Button) findViewById(R.id.stop_clean);
        buttonChangeFile = (Button) findViewById(R.id.choose_file);
        buttonNewRecording = (Button) findViewById(R.id.new_recording);
        switchModel = (Button) findViewById(R.id.switch_model);
        runDenoiser = (Button) findViewById(R.id.run);
        cleanSpectrogram = (SpectrogramView) findViewById(R.id.clean_SV);
        originalSpectrogram = (SpectrogramView) findViewById(R.id.original_SV);

        try{
            modelNames = getAssets().list("models");
        } catch (IOException e){
            Toast.makeText(MainActivity.this,"models folder not found", Toast.LENGTH_SHORT).show();
        }
        modelName = modelNames[0];
        modelLabel.setText(modelName.substring(0,modelName.length()-3));
        tfHelper = new TensorFlowInferenceInterface(getAssets(), "models/" + modelName);

        playOriginal.setEnabled(false);
        stopOriginal.setEnabled(false);
        playClean.setEnabled(false);
        stopClean.setEnabled(false);
        runDenoiser.setEnabled(false);

        playOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {
                if (audioFilePathOriginal != null) {
                    playOriginal.setEnabled(false);
                    stopOriginal.setEnabled(true);
                    originalMP = new MediaPlayer();
                    originalMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            stopOriginal.setEnabled(false);
                            playOriginal.setEnabled(true);
                            performOnEnd(mediaPlayer);
                        }
                    });
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(audioFilePathOriginal);

                        originalMP.setDataSource(fis.getFD());
                        originalMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        originalMP.prepare();
                        originalMP.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException ignore) {
                            }
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Record new audio or choose file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {
                if (audioFilePathClean != null) {
                    playClean.setEnabled(false);
                    stopClean.setEnabled(true);
                    cleanMP = new MediaPlayer();
                    cleanMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            stopClean.setEnabled(false);
                            playClean.setEnabled(true);
                            performOnEnd(mediaPlayer);
                        }
                    });
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(audioFilePathClean);

                        cleanMP.setDataSource(fis.getFD());
                        cleanMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        cleanMP.prepare();
                        cleanMP.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException ignore) {
                            }
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Denoise an audio sample first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopOriginal.setEnabled(false);
                playOriginal.setEnabled(true);
                performOnEnd(originalMP);
            }
        });

        stopClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopClean.setEnabled(false);
                playClean.setEnabled(true);
                performOnEnd(cleanMP);
            }
        });

        buttonChangeFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    Intent intent = new Intent(MainActivity.this, FileChooser.class);
                    startActivityForResult(intent, REQUEST_CODE_FILE);
                } else {
                    requestPermission();
                }
            }
        });

        buttonNewRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    Intent intent = new Intent(MainActivity.this, Recorder.class);
                    startActivityForResult(intent, REQUEST_CODE_RECORD);
                } else {
                    requestPermission();
                }
            }
        });
        switchModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchModelDialog().show();
            }
        });

        runDenoiser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DenoiseTask().execute();

            }
        });

        originalSpectrogram.clear();
        cleanSpectrogram.clear();
    }

    private class DenoiseTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            int windowSize = 512, hopSize = 256;
            int NUM_FRAME = 2;
            double[] signal = WavFile.readWavFile(audioFilePathOriginal);
            int width = signal.length / hopSize, height = hopSize + 1;
            float[] inputTensor = SpecUtils.forward(signal);
            float[] outputTensor = new float[width * height];

            tfHelper.feed(inputName, inputTensor, width, (NUM_FRAME * 2 + 1) * height);
            tfHelper.run(outputNames);
            tfHelper.fetch(outputName, outputTensor);

            double[] signalOut = SpecUtils.backward(outputTensor, signal);
            audioFilePathClean = cleanDir.getAbsolutePath() + "/" + getFileName(audioFilePathOriginal) + "-clean.wav";
            WavFile.writeWavFile(audioFilePathClean, signalOut);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            runDenoiser.setEnabled(false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            cleanName.setText(getFileName(audioFilePathClean) + ".wav");
            playClean.setEnabled(true);
            runDenoiser.setEnabled(true);
            Toast.makeText(MainActivity.this, "Finished Task", Toast.LENGTH_SHORT).show();
            cleanSpectrogram.resume(audioFilePathClean);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null){
            if (requestCode == REQUEST_CODE_FILE) {
                audioFilePathOriginal = FileChooser.decodeIntent(data);
            } else if(requestCode == REQUEST_CODE_RECORD){
                audioFilePathOriginal = Recorder.decodeIntent(data);
            }
        }

        if(audioFilePathOriginal != null && new File(audioFilePathOriginal).exists()){
            originalSpectrogram.resume(audioFilePathOriginal);
            String fileName = getFileName(audioFilePathOriginal);
            originalName.setText(fileName + ".wav");
            String cleanPath = cleanDir.getAbsolutePath() + "/" + fileName + "-clean.wav";
            if (new File(cleanPath).exists()){
                audioFilePathClean = cleanPath;
                cleanName.setText(fileName + "-clean.wav");
                playClean.setEnabled(true);
                cleanSpectrogram.resume(audioFilePathClean);
            } else {
                audioFilePathClean = null;
                cleanName.setText("Press Run to Denoise");
                playClean.setEnabled(false);
                stopClean.setEnabled(false);
                cleanSpectrogram.clear();
            }
            playOriginal.setEnabled(true);
            stopOriginal.setEnabled(false);
            runDenoiser.setEnabled(true);
        } else {
            playOriginal.setEnabled(false);
            stopOriginal.setEnabled(false);
            playClean.setEnabled(false);
            stopClean.setEnabled(false);
            runDenoiser.setEnabled(false);
            originalName.setText("No file chosen");
            cleanName.setText("No file chosen");
            originalSpectrogram.clear();
            cleanSpectrogram.clear();
        }


    }

    private void performOnEnd(MediaPlayer mediaPlayer){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Enable microphone and external file permissions", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    private String getFileName(String filePath){
        String[] split = filePath.split("/");
        String name = split[split.length-1];
        return name.substring(0,name.length()-4);
    }

    public Dialog switchModelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose a Model")
                .setItems(modelNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        modelName = modelNames[which];
                        tfHelper = new TensorFlowInferenceInterface(getAssets(), "models/" + modelName);
                        modelLabel.setText(modelName.substring(0,modelName.length()-3));
                    }
                });
        return builder.create();
    }
}

