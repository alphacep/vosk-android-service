package org.vosk.service.ui.model_list;

import static org.vosk.service.download.DownloadModelService.MODEL_FILE_ROOT_PATH;
import static org.vosk.service.download.Download.CLEAR;
import static org.vosk.service.download.Download.COMPLETE;
import static org.vosk.service.download.Download.RESTARTING;
import static org.vosk.service.download.Download.STARTING;
import static org.vosk.service.download.VoskClient.ServiceType.DOWNLOAD_MODEL_LIST;
import static org.vosk.service.utils.Tools.isServiceRunning;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.vosk.service.download.DownloadModelService;
import org.vosk.service.R;
import org.vosk.service.download.VoskClient;
import org.vosk.service.download.VoskService;
import org.vosk.service.download.Error;
import org.vosk.service.download.EventBus;
import org.vosk.service.utils.PreferenceConstants;
import org.vosk.service.download.FileHelper;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ModelListActivity extends AppCompatActivity {

    private final EventBus eventBus = EventBus.getInstance();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final VoskService service = VoskClient.getClient(null, DOWNLOAD_MODEL_LIST);
    private final Gson gson = new Gson();

    private ModelListAdapter modelListAdapter;
    private SharedPreferences sharedPreferences;

    private AlertDialog alertDialog;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<ModelItem> offlineModelList;
    public String modelDownloadingName = "";
    public static int progress = CLEAR;
    private boolean isOnline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_list);

        //Init fields
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        modelListAdapter = new ModelListAdapter(sharedPreferences);
        checkIfIsDownloading();
        loadOfflineModels();
        initViews();
        observeEvents();
        loadModels();
    }

    private void checkIfIsDownloading() {
        modelDownloadingName = sharedPreferences.getString(PreferenceConstants.DOWNLOADING_FILE, "");
        if (isOnline && !modelDownloadingName.equals("") && !isServiceRunning(this)) {
            progress = RESTARTING;
            startDownloadModelService();
        }
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_circular);

        recyclerView = findViewById(R.id.model_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(modelListAdapter);
        if (recyclerView.getItemAnimator() != null)
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    private void loadModels() {
        compositeDisposable.add(service.getModelList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        newDataset -> {
                            showList();
                            modelListAdapter.updateDataset(newDataset.stream().filter(it -> it.getType().equals("small") && !it.getObsolete()).collect(Collectors.toList()), offlineModelList.stream().filter(it -> !it.getName().equals(modelDownloadingName)).collect(Collectors.toList()));
                        },
                        error -> {
                            showList();
                            modelListAdapter.updateDataset(offlineModelList.stream().filter(it -> !it.getName().equals(modelDownloadingName)).collect(Collectors.toList()));
                        }));
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void observeEvents() {
        compositeDisposable.add(eventBus.getDownloadStatusObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(download -> {
                    if (download.getProgress() == COMPLETE) {
                        Toast.makeText(this, R.string.download_complete, Toast.LENGTH_SHORT).show();
                        loadOfflineModels();
                        progress = CLEAR;
                        sharedPreferences.edit()
                                .remove(PreferenceConstants.DOWNLOADING_FILE)
                                .apply();
                        modelListAdapter.updateOfflineModels(offlineModelList);
                    } else if (progress != download.getProgress()) {
                        progress = download.getProgress();
                        if (modelDownloadingName != null && modelListAdapter.getDataset().stream().anyMatch(it -> it.getName().equals(modelDownloadingName)))
                            modelListAdapter.getDataset().stream()
                                    .filter(it -> it.getName().equals(modelDownloadingName)).findFirst()
                                    .ifPresent(modelItem -> modelListAdapter.notifyItemChanged(modelListAdapter.getDataset().indexOf(modelItem)));
                    }
                }));

        compositeDisposable.add(eventBus.getDownloadStartObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(modelItem -> {
                    progress = STARTING;
                    //start service
                    startDownloadModelService();
                    modelDownloadingName = modelItem.getName();
                    sharedPreferences.edit().putString(PreferenceConstants.DOWNLOADING_FILE, modelItem.getName()).apply();
                    addOfflineModel(modelItem);
                }));

        compositeDisposable.add(eventBus.getModelSelectedObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::manageModelSelected));

        compositeDisposable.add(eventBus.geErrorObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleError));

        compositeDisposable.add(EventBus.getInstance().getConnectionEvent().subscribe(state -> {
                    if (state == NetworkInfo.State.CONNECTED) {
                        isOnline = true;
                        checkIfIsDownloading();
                    } else {
                        isOnline = false;
                    }
                })
        );

        compositeDisposable.add(ReactiveNetwork.observeNetworkConnectivity(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectivity -> {
                    if (connectivity.getState() == NetworkInfo.State.CONNECTED || connectivity.getState() == NetworkInfo.State.DISCONNECTED)
                        EventBus.getInstance().postConnectionEvent(connectivity.getState());
                })
        );

        compositeDisposable.add(eventBus.getDeleteDownloadedModelObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showDeleteModelDialog));
    }

    private void showDeleteModelDialog(ModelItem modelItem) {
        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            deleteOfflineModel(modelItem);
            modelListAdapter.updateOfflineModels(offlineModelList);
            Toast.makeText(this, getString(R.string.model_delete), Toast.LENGTH_LONG).show();
            dialog.dismiss();

        };
        showDialog(R.string.delete_model_dialog_title, R.string.delete_model_dialog_message, clickListener);
    }

    private void loadOfflineModels() {
        String offlineListJson = sharedPreferences.getString(PreferenceConstants.OFFLINE_LIST, "[]");
        offlineModelList = gson.fromJson(offlineListJson, new TypeToken<List<ModelItem>>() {
        }.getType());
    }

    private void addOfflineModel(ModelItem modelItem) {
        offlineModelList.add(modelItem);
        saveOfflineModelList();
    }

    private void deleteOfflineModel(ModelItem modelItem) {
        FileHelper.deleteFileOrDirectory(new File(MODEL_FILE_ROOT_PATH, modelItem.getName()));
        offlineModelList.stream()
                .filter(it -> it.getName().equals(modelItem.getName())).findFirst()
                .ifPresent(item -> offlineModelList.remove(item));
        saveOfflineModelList();
    }

    private void saveOfflineModelList() {
        Gson gson = new Gson();
        String offlineModelsJson = gson.toJson(offlineModelList);
        sharedPreferences.edit().putString(PreferenceConstants.OFFLINE_LIST, offlineModelsJson).apply();
    }

    private void handleError(Error error) {
        switch (error) {
            case CONNECTION: {
                Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                if (!modelDownloadingName.equals("")) {
                    modelListAdapter.notifyDataSetChanged();
                }
            }
            break;
            case WRITE_STORAGE:
                Toast.makeText(this, getString(R.string.write_storage_error), Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void startDownloadModelService() {
        if (!isServiceRunning(this)) {
            Intent service = new Intent(this, DownloadModelService.class);
            ContextCompat.startForegroundService(this, service);
        }
    }

    public void manageModelSelected(ModelItem modelItem) {
        if (isDownloaded(modelItem)) {
            selectDefaultModel(modelItem);
            modelListAdapter.notifyDataSetChanged();
        } else if (!sharedPreferences.contains(PreferenceConstants.DOWNLOADING_FILE)) {
            EventBus.getInstance().postDownloadStart(modelItem);
            modelListAdapter.notifyDataSetChanged();
        } else {
            showDialog(R.string.warning, R.string.wait_for_download, ((dialog, which) -> dialog.dismiss()));
        }
    }

    private void showDialog(int title, int message, Dialog.OnClickListener onClickListener) {
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(title))
                .setMessage(message)
                .setPositiveButton("Accept", onClickListener)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean isDownloaded(ModelItem modelItem) {
        if (!modelItem.getName().equals(sharedPreferences.getString(PreferenceConstants.DOWNLOADING_FILE, "")))
            return offlineModelList.stream().anyMatch(it -> it.getName().equals(modelItem.getName()));
        return false;
    }

    private void selectDefaultModel(ModelItem modelItem) {
        sharedPreferences.edit().putString(PreferenceConstants.ACTIVE_MODEL, modelItem.getName()).apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}