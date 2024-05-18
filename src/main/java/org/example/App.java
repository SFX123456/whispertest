package org.example;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello world!
 *
 */
public class App 
{
    static final long RECORD_TIME = 5000;  // 1 minute

    // the line from which audio data is captured
    TargetDataLine line;
    boolean listen = true;
    AudioInputStream audioInputStream;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    WhisperContext ctx ;
    WhisperFullParams params ;
    WhisperJNI whisperJNI;
    AudioFormat audioFormat; 
    AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
       
    }

    /**
     * Captures the sound and record into a WAV file
     */
    void start() {
        try {

            WhisperJNI.loadLibrary();
            WhisperJNI.setLibraryLogger(null);
            whisperJNI = new WhisperJNI();

            Path path = Paths.get("E:\\Coding\\Java\\WhisperTest\\src\\main\\java\\resources\\ggml-tiny.bin");
            ctx = whisperJNI.init(path);
             params = new WhisperFullParams();
        }
        catch (Exception e) {
            
        }
        try {
            audioFormat = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                System.exit(0);
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);


            line.start();


            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
            byte[] buffer = new byte[bufferSize];


            while (listen) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            }



        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } 
    }

    /**
     * Closes the target data line to finish capturing and recording
     */
    void finish() {
       
        
        byte[] audioData = byteArrayOutputStream.toByteArray();
        
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        
        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, getAudioFormat(), audioData.length / getAudioFormat().getFrameSize());
       
        try {
            analyzeAudio(audioInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main( String[] args ) throws Exception {
        Test test = new Test();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        bufferedReader.readLine();
        /*
        final App recorder = new App();

        // creates a new thread that waits for a specified
        // of time before stopping
        Thread stopper = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(RECORD_TIME);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    recorder.finish();
                }
            }
        });

        stopper.start();

        // start recording
        recorder.start();
        */
    }
    
    
    private void analyzeAudio(AudioInputStream audioInputStream) throws IOException {
        System.out.println("analyzing audio");
        
     
        // Calculate the number of frames to read
        int frameSize = audioFormat.getFrameSize();
        System.out.println(frameSize);
        int numBytes = (int) (audioInputStream.getFrameLength() * frameSize);
        System.out.println("numbytes " + numBytes);
        byte[] audioBytes = new byte[numBytes];

        int bytesRead = audioInputStream.read(audioBytes);
        // Convert the byte array to floating-point samples
        int bytesPerSample = audioFormat.getSampleSizeInBits() / 8;
        int numSamples = bytesRead / bytesPerSample;
        float[] samples = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            int start = i * bytesPerSample;
            int sampleValue = 0;
            for (int j = 0; j < bytesPerSample; j++) {
                sampleValue += (audioBytes[start + j] & 0xFF) << (8 * j);
            }
            samples[i] = (float) sampleValue / (float) Math.pow(2, audioFormat.getSampleSizeInBits() - 1);
        }
       
        audioInputStream.close();
        
        int result = whisperJNI.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        
        String text = whisperJNI.fullGetSegmentText(ctx, 0);
        System.out.println(text);
    }
  
}
