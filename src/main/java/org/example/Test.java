package org.example;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import java.awt.image.SampleModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bytedeco.ffmpeg.global.avcodec;

import static org.bytedeco.flycapture.global.FlyCapture2.FRAME_RATE;

public class Test {
    AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);

    }
    WhisperContext ctx ;
    WhisperFullParams params ;
    WhisperJNI whisperJNI; 
    public final static int SAMPLESARRAYSIZE = 1000000;
    public final static int SIZEBYTESSAMPLE = 2;
    public final static int SIZESAMPLES = 16000 * 10;
    public final static long TIMECYCLEAUDIOPROCESSING = 30000;
    public Object object = new Object();
    public Test() throws Exception, org.bytedeco.javacv.FrameGrabber.Exception {

        WhisperJNI.loadLibrary();
        WhisperJNI.setLibraryLogger(null);
        whisperJNI = new WhisperJNI();

    Path path = Paths.get("E:\\Coding\\Java\\WhisperTest\\src\\main\\java\\resources\\ggml-tiny.en.bin");
        ctx = whisperJNI.init(path);
        params = new WhisperFullParams();
        
        AudioFormat audioFormat = getAudioFormat();


        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        line.open(audioFormat);
        line.start();

        int audioBufferSize = (int) audioFormat.getSampleRate() * audioFormat.getChannels() * (int) TIMECYCLEAUDIOPROCESSING/1000;
        byte[] audioBytes = new byte[audioBufferSize];

        AudioInputStream isS = new AudioInputStream(line);
        float[] samples = new float[SAMPLESARRAYSIZE];
        int currentindex = 0;
        Boolean[] run = new Boolean[1];
        run[0] = true;
        /*a=
        new Thread(new Runnable() {
            @Override
            public void run()
            {


                ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                exec.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        run[0] = false;
                        float[] sam;
                        synchronized (object) {
                             sam = samples.clone();
                        }
                        run[0] = true;
                        analyzeAudio(sam);
                    }
                }, TIMECYCLEAUDIOPROCESSING, TIMECYCLEAUDIOPROCESSING, TimeUnit.MILLISECONDS);
            }
        }).start();
         */
        while (run[0]) {
            
                if (!run[0])continue;
                // Read from the line... non-blocking   
                int nBytesRead = isS.read(audioBytes, 0, line.available());

                if (nBytesRead > 0) {
                    int numSamples = nBytesRead / SIZEBYTESSAMPLE;
                   
                    for (int i = 0; i < numSamples; i++) {
                        int start = i * SIZEBYTESSAMPLE;
                        int sampleValue = 0;
                        for (int j = 0; j < SIZEBYTESSAMPLE; j++) {
                            sampleValue += (audioBytes[start + j] & 0xFF) << (8 * j);
                        }
                        samples[currentindex] = (float) sampleValue / (float) Math.pow(2, audioFormat.getSampleSizeInBits() - 1);
                        currentindex++;
                        if (currentindex % SIZESAMPLES == 0) {
                            float[] test = samples.clone();
                            Thread.ofVirtual().start(()-> {
                                    analyzeAudio(test);
                               
                            });
                            return;
                        }
                       
                    }
                    
                }
           
        }


      
    }

        private void analyzeAudio(float[] samples) {
        System.out.println("analyzing audio");


        // Calculate the number of frames to read

        int result = whisperJNI.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }

        String text = whisperJNI.fullGetSegmentText(ctx, 0);
        System.out.println(text);
    }

        // A really nice hardware accelerated component for our preview...
       
}