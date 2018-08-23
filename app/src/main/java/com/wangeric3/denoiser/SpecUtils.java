package com.wangeric3.denoiser;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Class containing functions for transforming audio signals for
 * use with the tensorflow model
 */
public class SpecUtils {
    /**
     * Returns a Hann window of specified length
     * @param windowLength the window length
     * @return an array of the Hann window
     */
    private static double[] hann(int windowLength) {
        double[] buffer = new double[windowLength];
        for(int i = 0; i < windowLength; i++) {
            buffer[i] = 0.5*(1-Math.cos((2*Math.PI*i)/(windowLength-1)));
        }
        return buffer;
    }

    /**
     * Calculates the short time fourier transform of an ausio signal
     * @param signal signal to calculate the STFT of
     * @return 2D stft matrix
     */
    public static double[][] stft(double signal[]){
        int signalLength = signal.length;
        int windowSize = 512;
        int hopSize = 256;
        int width = signalLength/hopSize;
        DoubleFFT_1D plan = new DoubleFFT_1D(windowSize);
        double[][] D = new double[width][windowSize];
        double[] window = hann(windowSize);
        int chunkPosition = 0;
        int readIndex;
        boolean bStop = false;
        int numChunks = 0;

        while(chunkPosition < signalLength && !bStop) {
            for(int i = 0; i < windowSize; i++) {
                readIndex = chunkPosition + i;
                if(readIndex < signalLength) {
                    D[numChunks][i] = signal[readIndex] * window[i];
                } else {
                    D[numChunks][i] = 0.0;
                    bStop = true;
                }
            }

            plan.realForward(D[numChunks]);

            chunkPosition += hopSize;
            numChunks++;
        }
        return D;
    }

    /**
     * Reconstructs the audio from an STFT matrix
     * @param D the STFT Matrix
     * @param signalLength length of the audio
     * @return the reconstructed audio signal
     */
    public static double[] istft(double D[][], int signalLength){
        int windowSize = 512;
        int hopSize = 256;
        int width = D.length;
        DoubleFFT_1D plan = new DoubleFFT_1D(windowSize);
        double[] signal_out = new double[signalLength];
        double[] squaredWindow = new double[signalLength];
        double[] window = hann(windowSize);
        int chunkPosition = 0;

        for (int numChunks=0;numChunks<width;numChunks++) {
            plan.realInverse(D[numChunks],true);
            for (int i = 0; i < windowSize; i++) {
                if(chunkPosition + i >= signalLength) break;
                signal_out[chunkPosition + i] += D[numChunks][i]*window[i];
                squaredWindow[chunkPosition + i] += Math.pow(window[i],2);
            }
            chunkPosition += hopSize;
        }
        for (int i = 0; i < signalLength; i++) {
            if (squaredWindow[i] > 0){
                signal_out[i] /= squaredWindow[i];
            }
        }

        return signal_out;
    }

    /**
     * Transforms an audio signal directly into the format required for feeding into the DDAE
     * @param signal the audio signal
     * @return array for input
     */
    public static float[] forward(double signal[]){
        double[][] D = stft(signal);
        int width = D.length, height = D[0].length / 2 + 1;
        int NUM_FRAME = 2;
        double[][] spec = new double[width][height];
        float[] inputTensor = new float[width * (NUM_FRAME * 2 + 1) * height];

        //Magnitude
        double eps = Math.ulp(1.0);
        for (int i = 0; i < width; i++) {
            spec[i][0] = Math.log10(Math.pow(D[i][0] + eps, 2));
            for (int j = 1; j < D[0].length / 2; j++) {
                spec[i][j] = Math.log10(Math.pow(D[i][2 * j] + eps, 2) + Math.pow(D[i][2 * j + 1] + eps, 2));
            }
            spec[i][D[0].length / 2] = Math.log10(Math.pow(D[i][1] + eps, 2));
        }

        //Norm
        double mean, var;
        DescriptiveStatistics ds;
        RealMatrix mat = MatrixUtils.createRealMatrix(spec);
        for (int i = 0; i < height; i++) {
            ds = new DescriptiveStatistics(mat.getColumn(i));
            mean = ds.getMean();
            var = ds.getVariance();
            for (int j = 0; j < width; j++) {
                spec[j][i] = (spec[j][i] - mean) / var;
            }
        }

        //Forwards/backwards and flatten
        int index;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < 5; j++) {
                index = i + (j - 2);
                for (int k = 0; k < height; k++) {
                    if (!(index < 0 || index >= width))
                        inputTensor[5 * height * i + height * j + k] = (float) spec[index][k];
                }
            }
        }
        return inputTensor;
    }

    /**
     * Reconstructs the output of the DDAE into the audio signal
     * @param outputTensor the output fetched from the DDAE
     * @param signal the original audio signal
     * @return the "clean" audio signal
     */
    public static double[] backward(float outputTensor[], double signal[]) {
        double[][] D = stft(signal);
        int width = D.length, height = D[0].length / 2 + 1;
        double r, p;
        for (int i = 0; i < width; i++) {
            //first and last element has 0 imaginary component
            D[i][0] = Math.sqrt(Math.pow(10, outputTensor[i * height])) * Math.cos(Math.atan2(0, D[i][0]));
            D[i][1] = Math.sqrt(Math.pow(10, outputTensor[(i + 1) * height - 1])) * Math.cos(Math.atan2(0, D[i][1]));
            for (int j = 1; j < height - 1; j++) {
                r = Math.sqrt(Math.pow(10, outputTensor[i * height + j]));
                p = Math.atan2(D[i][2 * j + 1], D[i][2 * j]);
                D[i][2 * j] = r * Math.cos(p);
                D[i][2 * j + 1] = r * Math.sin(p);
            }
        }
        return istft(D,signal.length);
    }
}
