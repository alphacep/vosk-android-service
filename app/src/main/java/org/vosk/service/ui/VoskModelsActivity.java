// Copyright 2019 Alpha Cephei Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.vosk.service.ui;

import static org.vosk.service.download.Download.RESTARTING;
import static org.vosk.service.utils.Tools.isServiceRunning;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.service.download.DownloadModelService;
import org.vosk.service.R;
import org.vosk.service.ui.selector.ModelListActivity;
import org.vosk.service.download.Error;
import org.vosk.service.download.EventBus;
import org.vosk.service.utils.PreferenceConstants;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class VoskModelsActivity extends Activity {

    public static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public static final int PERMISSIONS_REQUEST_ALL_FILES_ACCESS = 2;

    private CompositeDisposable compositeDisposable;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.navigate_model_lists).setOnClickListener(view -> {
            int storagePermissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
            if (storagePermissionCheck != PackageManager.PERMISSION_GRANTED)
                navigateToModelList();
            else
                requestAllFilesAccessPermission();
        });

        LibVosk.setLogLevel(LogLevel.INFO);
    }

    private void observeEvents() {
        compositeDisposable.add(EventBus.getInstance().getConnectionEvent().subscribe(state -> {
                    if (state == NetworkInfo.State.CONNECTED) {
                        checkIfIsDownloading();
                    }
                })
        );

        compositeDisposable.add(
                EventBus.getInstance().geErrorObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleError)
        );

        compositeDisposable.add(ReactiveNetwork.observeNetworkConnectivity(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectivity -> {
                    if (connectivity.getState() == NetworkInfo.State.CONNECTED || connectivity.getState() == NetworkInfo.State.DISCONNECTED)
                        EventBus.getInstance().postConnectionEvent(connectivity.getState());
                })
        );
    }

    private void handleError(Error error) {
        switch (error) {
            case CONNECTION:
                Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                break;
            case WRITE_STORAGE:
                Toast.makeText(this, getString(R.string.write_storage_error), Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void checkIfIsDownloading() {
        String modelDownloadingName = sharedPreferences.getString(PreferenceConstants.DOWNLOADING_FILE, "");
        if (!modelDownloadingName.equals("") && !isServiceRunning(this)) {
            ModelListActivity.progress = RESTARTING;
            startDownloadModelService();
        }
    }

    private void startDownloadModelService() {
        if (!isServiceRunning(this)) {
            Intent service = new Intent(this, DownloadModelService.class);
            ContextCompat.startForegroundService(this, service);
        }
    }

    private void navigateToModelList() {
        Intent intent = new Intent(this, ModelListActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.mic_permission_error), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == PERMISSIONS_REQUEST_ALL_FILES_ACCESS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigateToModelList();
            } else {
                Toast.makeText(this, getString(R.string.file_access_permission_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        compositeDisposable = new CompositeDisposable();
        observeEvents();
        checkPermissions();
    }

    private void checkPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        compositeDisposable.clear();
    }

    private void requestAllFilesAccessPermission() {
        // Check if user has given all files access permission to record audio, init model after permission is granted
        if (Build.VERSION.SDK_INT >= 30) {
            Log.i(VoskModelsActivity.class.getName(), "API level >= 30");
            if (!Environment.isExternalStorageManager()) {
                // Request permission
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_ALL_FILES_ACCESS);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(this, "Failed to request all files access permission", Toast.LENGTH_LONG).show();
                }
            } else {
                navigateToModelList();
            }
        } else {
            Log.i(VoskModelsActivity.class.getName(), "API level < 30");
            // Request permission
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_ALL_FILES_ACCESS);
            } else {
                navigateToModelList();
            }
        }
    }
}
