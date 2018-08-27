package com.wangeric3.denoiser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static java.lang.Math.abs;

public class SpectrogramView extends SurfaceView {

    Context mContext;
    SurfaceHolder mSurfaceHolder;
    Paint mPaint = new Paint();
    Canvas mCanvas;
    String filePath;
    int process;

    public SpectrogramView(Context context) {
        super(context);
        init(context);
    }

    public SpectrogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SpectrogramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                switch  (process){
                    case 0:
                        new SpectrogramTask().execute();
                        break;
                    case 1:
                        break;
                    case 2:
                        mCanvas = mSurfaceHolder.lockCanvas();
                        mCanvas.drawColor(Color.rgb(150,150,150));
                        mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                        break;
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

        });
    }

    public void resume(String filePath) {
        process = 0;
        this.filePath = filePath;
        if (mSurfaceHolder.getSurface().isValid()) new SpectrogramTask().execute();
    }

    public void clear(){
        process = 2;
        if (mSurfaceHolder.getSurface().isValid()){
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.rgb(150,150,150));
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    private class SpectrogramTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (mSurfaceHolder.getSurface().isValid()) {
                mCanvas = mSurfaceHolder.lockCanvas();
                int vWidth = mCanvas.getWidth();
                int vHeight = mCanvas.getHeight();
                mCanvas.translate(0, vHeight);
                double D[][] = SpecUtils.stft(WavFile.readWavFile(filePath));
                int width = D.length, height = D[0].length / 2 + 1;
                float tileWidth = (float) vWidth / (float) width, tileHeight = (float) vHeight / (float) height;
                double[][] Sxx = new double[width][height];

                double max = Math.log10(Math.pow(D[0][0], 2));
                double min = Double.POSITIVE_INFINITY;
                for (int i = 0; i < width; i++) {
                    Sxx[i][0] = Math.log10(Math.pow(D[i][0], 2));
                    if (Sxx[i][0] > max) max = Sxx[i][0];
                    else if (Sxx[i][0] < min && Sxx[i][0] > -20) min = Sxx[i][0];
                    for (int j = 1; j < D[0].length / 2; j++) {
                        Sxx[i][j] = Math.log10(Math.pow(D[i][2 * j], 2) + Math.pow(D[i][2 * j + 1], 2));
                        if (Sxx[i][j] > max) max = Sxx[i][j];
                        else if (Sxx[i][j] < min && Sxx[i][j] > -20) min = Sxx[i][j];
                    }
                    Sxx[i][D[0].length / 2] = Math.log10(Math.pow(D[i][1], 2));
                    if (Sxx[i][D[0].length / 2] > max) max = Sxx[i][D[0].length / 2];
                    else if (Sxx[i][D[0].length / 2] < min && Sxx[i][D[0].length / 2] > -20)
                        min = Sxx[i][D[0].length / 2];
                }

                int[] rgb;
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        rgb = HSLtoRGB((float) Sxx[i][j], (float) min, (float) max);
                        mPaint.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                        mCanvas.drawRect(i * tileWidth, -(j + 1) * tileHeight, (i + 1) * tileWidth, -j * tileHeight, mPaint);
                    }
                }

                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
            return null;
        }

        private int[] HSLtoRGB(float power, float min, float max){
            int[] rgb = new int[3];
            float L = (power-min)/(max-min);
            float C = (1- abs(2*L-1)) * 1;
            float X = C * (1-abs((245/60) % 2 - 1));
            float m = L - C/2;

            rgb[0] = Math.round((X + m) * 255);
            rgb[1] = Math.round((m) * 255);
            rgb[2] = Math.round((C + m) * 255);
            return rgb;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

}
