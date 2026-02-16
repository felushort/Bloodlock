package com.void.echo.util;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Advanced signal processing utilities for behavioral analysis
 * 
 * Provides specialized methods for:
 * - Frequency domain analysis
 * - Wavelet transforms
 * - Filtering and noise reduction
 * - Pattern extraction
 */
public class SignalProcessor {
    
    private static final double EPSILON = 1e-10;
    
    /**
     * Apply Gaussian filter to smooth signal
     */
    public static double[] gaussianFilter(double[] signal, double sigma) {
        if (signal.length == 0 || sigma <= 0) return signal.clone();
        
        // Calculate kernel size
        int kernelSize = (int) Math.ceil(6 * sigma);
        if (kernelSize % 2 == 0) kernelSize++;
        
        // Generate Gaussian kernel
        double[] kernel = new double[kernelSize];
        int center = kernelSize / 2;
        double sum = 0.0;
        
        for (int i = 0; i < kernelSize; i++) {
            double x = i - center;
            kernel[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        
        // Normalize kernel
        for (int i = 0; i < kernelSize; i++) {
            kernel[i] /= sum;
        }
        
        // Apply convolution
        double[] filtered = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            double value = 0.0;
            double weight = 0.0;
            
            for (int j = 0; j < kernelSize; j++) {
                int index = i - center + j;
                if (index >= 0 && index < signal.length) {
                    value += signal[index] * kernel[j];
                    weight += kernel[j];
                }
            }
            
            filtered[i] = weight > 0 ? value / weight : signal[i];
        }
        
        return filtered;
    }
    
    /**
     * Apply median filter (good for removing outliers while preserving edges)
     */
    public static double[] medianFilter(double[] signal, int windowSize) {
        if (signal.length == 0 || windowSize < 1) return signal.clone();
        if (windowSize % 2 == 0) windowSize++;
        
        double[] filtered = new double[signal.length];
        int halfWindow = windowSize / 2;
        
        for (int i = 0; i < signal.length; i++) {
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(signal.length, i + halfWindow + 1);
            
            double[] window = new double[end - start];
            System.arraycopy(signal, start, window, 0, end - start);
            
            filtered[i] = MathUtil.median(window);
        }
        
        return filtered;
    }
    
    /**
     * Calculate spectrogram (time-frequency representation)
     * Returns 2D array [time][frequency]
     */
    public static double[][] spectrogram(double[] signal, int windowSize, int hopSize) {
        if (signal.length < windowSize) return new double[0][0];
        
        int numWindows = (signal.length - windowSize) / hopSize + 1;
        int freqBins = windowSize / 2;
        
        double[][] spectrogram = new double[numWindows][freqBins];
        
        for (int i = 0; i < numWindows; i++) {
            int start = i * hopSize;
            int end = Math.min(start + windowSize, signal.length);
            
            double[] window = new double[windowSize];
            System.arraycopy(signal, start, window, 0, end - start);
            
            // Apply Hamming window
            for (int j = 0; j < window.length; j++) {
                window[j] *= 0.54 - 0.46 * Math.cos(2 * Math.PI * j / (windowSize - 1));
            }
            
            // Calculate power spectrum for this window
            double[] power = MathUtil.powerSpectrum(window);
            System.arraycopy(power, 0, spectrogram[i], 0, Math.min(power.length, freqBins));
        }
        
        return spectrogram;
    }
    
    /**
     * Calculate spectral centroid (center of mass of spectrum)
     * Higher value = higher frequency content
     */
    public static double spectralCentroid(double[] signal) {
        double[] spectrum = MathUtil.powerSpectrum(signal);
        
        double weightedSum = 0.0;
        double sum = 0.0;
        
        for (int i = 0; i < spectrum.length; i++) {
            weightedSum += i * spectrum[i];
            sum += spectrum[i];
        }
        
        return sum > 0 ? weightedSum / sum : 0.0;
    }
    
    /**
     * Calculate spectral rolloff (frequency below which X% of energy is contained)
     */
    public static double spectralRolloff(double[] signal, double threshold) {
        double[] spectrum = MathUtil.powerSpectrum(signal);
        
        double totalEnergy = 0.0;
        for (double s : spectrum) {
            totalEnergy += s;
        }
        
        double targetEnergy = totalEnergy * threshold;
        double cumulativeEnergy = 0.0;
        
        for (int i = 0; i < spectrum.length; i++) {
            cumulativeEnergy += spectrum[i];
            if (cumulativeEnergy >= targetEnergy) {
                return (double) i / spectrum.length;
            }
        }
        
        return 1.0;
    }
    
    /**
     * Calculate spectral flux (measure of how quickly spectrum changes)
     */
    public static double spectralFlux(double[] signal1, double[] signal2) {
        double[] spectrum1 = MathUtil.powerSpectrum(signal1);
        double[] spectrum2 = MathUtil.powerSpectrum(signal2);
        
        int len = Math.min(spectrum1.length, spectrum2.length);
        double flux = 0.0;
        
        for (int i = 0; i < len; i++) {
            double diff = spectrum2[i] - spectrum1[i];
            flux += diff * diff;
        }
        
        return Math.sqrt(flux);
    }
    
    /**
     * Zero-crossing rate (how often signal crosses zero)
     * High ZCR = noisy/high frequency, Low ZCR = smooth/low frequency
     */
    public static double zeroCrossingRate(double[] signal) {
        if (signal.length < 2) return 0.0;
        
        int crossings = 0;
        for (int i = 1; i < signal.length; i++) {
            if ((signal[i] >= 0 && signal[i-1] < 0) || 
                (signal[i] < 0 && signal[i-1] >= 0)) {
                crossings++;
            }
        }
        
        return (double) crossings / (signal.length - 1);
    }
    
    /**
     * Calculate envelope of signal (smoothed amplitude)
     */
    public static double[] envelope(double[] signal, int windowSize) {
        double[] envelope = new double[signal.length];
        int halfWindow = windowSize / 2;
        
        for (int i = 0; i < signal.length; i++) {
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(signal.length, i + halfWindow + 1);
            
            double maxAbs = 0.0;
            for (int j = start; j < end; j++) {
                double abs = Math.abs(signal[j]);
                if (abs > maxAbs) {
                    maxAbs = abs;
                }
            }
            
            envelope[i] = maxAbs;
        }
        
        return envelope;
    }
    
    /**
     * Detect peaks in signal
     * Returns indices of peaks
     */
    public static int[] detectPeaks(double[] signal, double threshold, int minDistance) {
        if (signal.length < 3) return new int[0];
        
        java.util.List<Integer> peaks = new java.util.ArrayList<>();
        
        for (int i = 1; i < signal.length - 1; i++) {
            // Check if local maximum
            if (signal[i] > signal[i-1] && signal[i] > signal[i+1] && signal[i] > threshold) {
                // Check minimum distance from last peak
                if (peaks.isEmpty() || i - peaks.get(peaks.size() - 1) >= minDistance) {
                    peaks.add(i);
                }
            }
        }
        
        return peaks.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Calculate harmonic-to-noise ratio
     * Higher = more harmonic (periodic), Lower = more noisy
     */
    public static double harmonicToNoiseRatio(double[] signal) {
        if (signal.length < 4) return 0.0;
        
        // Detect fundamental period
        int period = MathUtil.detectPeriod(signal, 0.3);
        if (period == 0) return 0.0;
        
        // Calculate harmonic and noise components
        double harmonicEnergy = 0.0;
        double totalEnergy = 0.0;
        
        for (int i = 0; i < signal.length; i++) {
            totalEnergy += signal[i] * signal[i];
            
            // Check if this index aligns with period
            if (i % period < period / 4) {
                harmonicEnergy += signal[i] * signal[i];
            }
        }
        
        double noiseEnergy = totalEnergy - harmonicEnergy;
        
        return noiseEnergy > EPSILON ? 10 * Math.log10(harmonicEnergy / noiseEnergy) : 0.0;
    }
    
    /**
     * Calculate crest factor (peak-to-RMS ratio)
     * Measures "peakiness" of signal
     */
    public static double crestFactor(double[] signal) {
        if (signal.length == 0) return 0.0;
        
        double peak = 0.0;
        double sumSquares = 0.0;
        
        for (double s : signal) {
            double abs = Math.abs(s);
            if (abs > peak) peak = abs;
            sumSquares += s * s;
        }
        
        double rms = Math.sqrt(sumSquares / signal.length);
        
        return rms > EPSILON ? peak / rms : 0.0;
    }
    
    /**
     * Band-pass filter (keep frequencies in specified range)
     */
    public static double[] bandPassFilter(double[] signal, double lowFreq, double highFreq, double samplingRate) {
        if (signal.length == 0) return signal.clone();
        
        // Simple implementation using FFT
        int n = nextPowerOf2(signal.length);
        double[] padded = new double[n];
        System.arraycopy(signal, 0, padded, 0, signal.length);
        
        try {
            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] spectrum = fft.transform(padded, TransformType.FORWARD);
            
            // Apply filter in frequency domain
            for (int i = 0; i < spectrum.length; i++) {
                double freq = (double) i * samplingRate / n;
                if (freq < lowFreq || freq > highFreq) {
                    spectrum[i] = Complex.ZERO;
                }
            }
            
            // Transform back
            Complex[] filtered = fft.transform(spectrum, TransformType.INVERSE);
            
            double[] result = new double[signal.length];
            for (int i = 0; i < signal.length; i++) {
                result[i] = filtered[i].getReal();
            }
            
            return result;
        } catch (Exception e) {
            return signal.clone();
        }
    }
    
    /**
     * Calculate cross-correlation between two signals
     * Used to detect similarity and time lag
     */
    public static double[] crossCorrelation(double[] signal1, double[] signal2, int maxLag) {
        if (signal1.length == 0 || signal2.length == 0) return new double[0];
        
        int n = Math.min(signal1.length, signal2.length);
        maxLag = Math.min(maxLag, n - 1);
        
        double[] correlation = new double[2 * maxLag + 1];
        
        double mean1 = MathUtil.mean(signal1);
        double mean2 = MathUtil.mean(signal2);
        double std1 = MathUtil.standardDeviation(signal1);
        double std2 = MathUtil.standardDeviation(signal2);
        
        if (std1 < EPSILON || std2 < EPSILON) return correlation;
        
        for (int lag = -maxLag; lag <= maxLag; lag++) {
            double sum = 0.0;
            int count = 0;
            
            for (int i = 0; i < n; i++) {
                int j = i + lag;
                if (j >= 0 && j < signal2.length) {
                    sum += ((signal1[i] - mean1) / std1) * ((signal2[j] - mean2) / std2);
                    count++;
                }
            }
            
            correlation[lag + maxLag] = count > 0 ? sum / count : 0.0;
        }
        
        return correlation;
    }
    
    /**
     * Calculate time-varying variance (volatility)
     */
    public static double[] timeVaryingVariance(double[] signal, int windowSize) {
        if (signal.length < windowSize) return new double[signal.length];
        
        double[] variance = new double[signal.length];
        
        for (int i = windowSize; i < signal.length; i++) {
            double[] window = new double[windowSize];
            System.arraycopy(signal, i - windowSize, window, 0, windowSize);
            variance[i] = MathUtil.variance(window);
        }
        
        // Fill initial values
        double initialVar = variance[windowSize];
        for (int i = 0; i < windowSize; i++) {
            variance[i] = initialVar;
        }
        
        return variance;
    }
    
    /**
     * Next power of 2
     */
    private static int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }
    
    /**
     * Calculate rate of change
     */
    public static double[] rateOfChange(double[] signal) {
        if (signal.length < 2) return new double[signal.length];
        
        double[] roc = new double[signal.length];
        roc[0] = 0.0;
        
        for (int i = 1; i < signal.length; i++) {
            roc[i] = signal[i] - signal[i-1];
        }
        
        return roc;
    }
    
    /**
     * Calculate momentum (rate of rate of change)
     */
    public static double[] momentum(double[] signal) {
        double[] roc = rateOfChange(signal);
        return rateOfChange(roc);
    }
}
