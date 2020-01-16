package cn.qiuxiang.react.recording;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.RequiresApi;

class RecordingModule extends ReactContextBaseJavaModule {
    private static AudioRecord audioRecord;
    private final ReactApplicationContext reactContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private boolean running;
    private int bufferSize;
    private Thread recordingThread;
    private boolean isFloat = false;

    RecordingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "Recording";
    }

    @ReactMethod
    public void init(ReadableMap options) {
        if (eventEmitter == null) {
            eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }

        if (running || (recordingThread != null && recordingThread.isAlive())) {
            return;
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }

        // for parameter description, see
        // https://developer.android.com/reference/android/media/AudioRecord.html

        int sampleRateInHz = 44100;
        if (options.hasKey("sampleRate")) {
            sampleRateInHz = options.getInt("sampleRate");
        }

        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        if (options.hasKey("channelsPerFrame")) {
            int channelsPerFrame = options.getInt("channelsPerFrame");

            // every other case --> CHANNEL_IN_MONO
            if (channelsPerFrame == 2) {
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            }
        }

        // we support only 8-bit and 16-bit PCM
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (options.hasKey("bitsPerChannel")) {
            int bitsPerChannel = options.getInt("bitsPerChannel");
            isFloat = false;

            if (bitsPerChannel == 8) {
                audioFormat = AudioFormat.ENCODING_PCM_8BIT;
            } else if (bitsPerChannel == 32) {
                audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
                isFloat = true;
            }
        }

        if (options.hasKey("bufferSize")) {
            this.bufferSize = options.getInt("bufferSize");
        } else {
            this.bufferSize = 8192;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.CAMCORDER,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                this.bufferSize * 2);

        if (Build.VERSION.SDK_INT >= 16)
            NoiseSuppressor.create(audioRecord.getAudioSessionId());
        if (Build.VERSION.SDK_INT >= 16)
            AutomaticGainControl.create(audioRecord.getAudioSessionId());

        recordingThread = new Thread(new Runnable() {
            public void run() {
                recording();
            }
        }, "RecordingThread");
    }

    @ReactMethod
    public void start() {
        if (!running && audioRecord != null && recordingThread != null) {
            running = true;
            audioRecord.startRecording();
            recordingThread.start();
        }
    }

    @ReactMethod
    public void stop() {
        if (audioRecord != null) {
            running = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void recording() {
        if (isFloat && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
//            Log.d("recorder", "setting buffer size " + bufferSize);
            float buffer[] = new float[bufferSize / 4];
            while (running && !reactContext.getCatalystInstance().isDestroyed()) {
                audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                byte[] bytesArray = new byte[buffer.length * 4];
                ByteBuffer.wrap(bytesArray).order(ByteOrder.nativeOrder()).asFloatBuffer().put(buffer);
                String encoded = Base64.encodeToString(bytesArray, Base64.NO_WRAP);
                eventEmitter.emit("recording", encoded);
            }
        } else {
            short buffer[] = new short[bufferSize];
            while (running && !reactContext.getCatalystInstance().isDestroyed()) {
                audioRecord.read(buffer, 0, bufferSize);
                byte[] bytesArray = short2byte(buffer);
                String encoded = Base64.encodeToString(bytesArray, Base64.NO_WRAP);
                eventEmitter.emit("recording", encoded);
            }
        }
    }

    public static byte[] short2byte(short[] paramArrayOfshort) {
        int j = paramArrayOfshort.length;
        byte[] arrayOfByte = new byte[j * 2];
        for (int i = 0; ; i++) {
            if (i >= j)
                return arrayOfByte;
            arrayOfByte[i * 2] = (byte) (byte) (paramArrayOfshort[i] & 0xFF);
            arrayOfByte[i * 2 + 1] = (byte) (byte) (paramArrayOfshort[i] >> 8);
            paramArrayOfshort[i] = 0;
        }
    }
}
