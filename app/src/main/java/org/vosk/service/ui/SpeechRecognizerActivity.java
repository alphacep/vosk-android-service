// Copyright 2020 Ciaran O'Reilly
// Copyright 2011-2020, Institute of Cybernetics at Tallinn University of Technology
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package org.vosk.service.ui;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import org.vosk.service.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeechRecognizerActivity extends AppCompatActivity {
    protected static final String TAG = SpeechRecognizerActivity.class.getSimpleName();

    private SpeechRecognizer speechRecognizer;

    private static final String MSG = "MSG";
    private static final int MSG_TOAST = 1;
    private static final int MSG_RESULT_ERROR = 2;

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    protected static class SimpleMessageHandler extends Handler {
        private final WeakReference<SpeechRecognizerActivity> mRef;

        private SimpleMessageHandler(Looper looper, SpeechRecognizerActivity activity) {
            super(looper);
            mRef = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            SpeechRecognizerActivity outerClass = mRef.get();
            if (outerClass != null) {
                Bundle b = msg.getData();
                String msgAsString = b.getString(MSG);
                switch (msg.what) {
                    case MSG_TOAST:
                        outerClass.toast(msgAsString);
                        break;
                    case MSG_RESULT_ERROR:
                        outerClass.showError(msgAsString);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    protected static Message createMessage(String str) {
        Bundle b = new Bundle();
        b.putString(MSG, str);
        Message msg = Message.obtain();
        msg.what = SpeechRecognizerActivity.MSG_TOAST;
        msg.setData(b);
        return msg;
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    void showError(String msg) {
        Log.d(TAG, msg);
    }

    void setupRecognizer() {
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString().replace("_","-"));
        speechRecognizer.startListening(speechRecognizerIntent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speech_recognizer_activity);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                speechRecognizer.stopListening();
            }

            @Override
            public void onError(int i) {
                showError();
            }

            @Override
            public void onResults(Bundle bundle) {
                Log.i(TAG, "onResults");
                ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.i(TAG, results.get(0));
                returnResults(results);
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.i(TAG, "onPartialResults");
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.i(TAG, data.get(0));
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.d(TAG, bundle.toString());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        else {
            setupRecognizer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        speechRecognizer.destroy();
    }


    public void startSpeechSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.start_speech_effect);
        mp.start();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length - 1; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
        setupRecognizer();
    }

    private void returnResults(List<String> results) {
        Handler handler = new SimpleMessageHandler(Looper.getMainLooper(), this);

        Intent incomingIntent = getIntent();
        Log.d(TAG, incomingIntent.toString());
        Bundle extras = incomingIntent.getExtras();
        if (extras == null) {
            return;
        }
        Log.d(TAG, extras.toString());
        PendingIntent pendingIntent = getPendingIntent(extras);
        if (pendingIntent == null) {
            Log.d(TAG, "No pending intent, setting result intent.");
            setResultIntent(results);
        } else {
            Log.d(TAG, pendingIntent.toString());

            Bundle bundle = extras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE);
            if (bundle == null) {
                bundle = new Bundle();
            }

            Intent intent = new Intent();
            intent.putExtras(bundle);
            handler.sendMessage(
                    createMessage(String.format(getString(R.string.recognized), results.get(0))));
            try {
                Log.d(TAG, "Sending result via pendingIntent");
                pendingIntent.send(this, AppCompatActivity.RESULT_OK, intent);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
                handler.sendMessage(createMessage(e.getMessage()));
            }
        }
        finish();
    }

    private void showError() {
        toast("Error loading recognizer");
    }

    private void setResultIntent(List<String> matches) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, new ArrayList<>(matches));
        setResult(Activity.RESULT_OK, intent);
    }

    private PendingIntent getPendingIntent(Bundle extras) {
        Parcelable extraResultsPendingIntentAsParceable = extras
                .getParcelable(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT);
        if (extraResultsPendingIntentAsParceable != null) {
            if (extraResultsPendingIntentAsParceable instanceof PendingIntent) {
                return (PendingIntent) extraResultsPendingIntentAsParceable;
            }
        }
        return null;
    }
}