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

import static org.vosk.service.download.DownloadModelService.MODEL_FILE_ROOT_PATH;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.vosk.service.R;
import org.vosk.service.utils.PreferenceConstants;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SpeechRecognizerActivity extends AppCompatActivity {
    protected static final String TAG = SpeechRecognizerActivity.class.getSimpleName();

    private static final String MSG = "MSG";
    private static final int MSG_TOAST = 1;
    private static final int MSG_RESULT_ERROR = 2;
    public static final Integer RecordAudioRequestCode = 1;

    private SharedPreferences sharedPreferences;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //Vosk
    private Model model;
    private SpeechService speechService;
    RecognitionListener listener;

    protected static class SimpleMessageHandler extends Handler {
        private final WeakReference<SpeechRecognizerActivity> mRef;

        private SimpleMessageHandler(SpeechRecognizerActivity c) {
            mRef = new WeakReference<>(c);
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

    protected static Message createMessage(int type, String str) {
        Bundle b = new Bundle();
        b.putString(MSG, str);
        Message msg = Message.obtain();
        msg.what = type;
        msg.setData(b);
        return msg;
    }

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    void showError(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speech_recognizer_activity);
        LibVosk.setLogLevel(LogLevel.INFO);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            initModel();
        }


        listener = new RecognitionListener() {
            @Override
            public void onPartialResult(String s) {
                if (!s.contains("\"partial\" : \"\"")) {
                    try {
                        JSONObject json = new JSONObject(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onResult(String s) {
                try {
                    JSONObject json = new JSONObject(s);
                    String resultText = json.getString("text");
                    List<String> results = Collections.singletonList(resultText);
                    returnResults(results);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinalResult(String s) {
                speechService.stop();
            }

            @Override
            public void onError(Exception e) {
                showError();
            }

            @Override
            public void onTimeout() {

            }
        };
    }

    private void initModel() {
        if (sharedPreferences.contains(PreferenceConstants.ACTIVE_MODEL)) {
            File outputFile = new File(MODEL_FILE_ROOT_PATH, sharedPreferences.getString(PreferenceConstants.ACTIVE_MODEL, "") + "/" + sharedPreferences.getString(PreferenceConstants.ACTIVE_MODEL, ""));

            compositeDisposable.add(Single.fromCallable(() -> new Model(outputFile.getAbsolutePath()))
                    .doOnSuccess(model1 -> this.model = model1)
                    .delay(1, TimeUnit.MICROSECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(model1 -> startSpeech(), Throwable::printStackTrace));
        } else
            StorageService.unpack(this, "model-en-us", "model",
                    (model) -> {
                        this.model = model;
                        startSpeech();
                    },
                    Throwable::printStackTrace);
    }

    public void startSpeechSound() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.start_speech_effect);
        mp.start();
    }

    private void startSpeech() {
        startSpeechSound();
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
        compositeDisposable.clear();
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
    }

    private void returnResults(List<String> results) {
        Handler handler = new SimpleMessageHandler(this);

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
            setResultIntent(handler, results);
        } else {
            Log.d(TAG, pendingIntent.toString());

            Bundle bundle = extras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE);
            if (bundle == null) {
                bundle = new Bundle();
            }

            Intent intent = new Intent();
            intent.putExtras(bundle);
            handler.sendMessage(
                    createMessage(MSG_TOAST, String.format(getString(R.string.recognized), results.get(0))));
            try {
                Log.d(TAG, "Sending result via pendingIntent");
                pendingIntent.send(this, AppCompatActivity.RESULT_OK, intent);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
                handler.sendMessage(createMessage(MSG_TOAST, e.getMessage()));
            }
        }
        finish();
    }

    private void showError() {
        toast("Error loading recognizer");
    }

    private void setResultIntent(final Handler handler, List<String> matches) {
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