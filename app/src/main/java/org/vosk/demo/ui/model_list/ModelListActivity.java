package org.vosk.demo.ui.model_list;

import static org.vosk.demo.api.Download.CLEAR;
import static org.vosk.demo.api.Download.COMPLETE;
import static org.vosk.demo.api.Download.RESTARTING;
import static org.vosk.demo.api.Download.STARTING;
import static org.vosk.demo.api.VoskClient.ServiceType.DOWNLOAD_MODEL_LIST;
import static org.vosk.demo.utils.Tools.isServiceRunning;

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

import org.vosk.demo.DownloadModelService;
import org.vosk.demo.R;
import org.vosk.demo.api.VoskClient;
import org.vosk.demo.api.VoskService;
import org.vosk.demo.utils.Error;
import org.vosk.demo.utils.EventBus;
import org.vosk.demo.utils.PreferenceConstants;

import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ModelListActivity extends AppCompatActivity {

    private final EventBus eventBus = EventBus.getInstance();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final VoskService service = VoskClient.getClient(null, DOWNLOAD_MODEL_LIST);

    private ModelListAdapter modelListAdapter;
    private SharedPreferences sharedPreferences;

    private AlertDialog alertDialog;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<ModelItem> offlineModels;
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
                            modelListAdapter.updateDataset(newDataset.stream().filter(it -> it.getType().equals("small") && !it.getObsolete()).collect(Collectors.toList()), offlineModels.stream().filter(it -> !it.getName().equals(modelDownloadingName)).collect(Collectors.toList()));
                        },
                        error -> {
                            showList();
                            modelListAdapter.updateDataset(offlineModels.stream().filter(it -> !it.getName().equals(modelDownloadingName)).collect(Collectors.toList()));
                        }));
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void loadOfflineModels() {
        String offlineListJson = sharedPreferences.getString(PreferenceConstants.OFFLINE_LIST, "[]");
        Gson gson = new Gson();
        offlineModels = gson.fromJson(offlineListJson, new TypeToken<List<ModelItem>>() {
        }.getType());
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
                        modelListAdapter.updateOfflineModels(offlineModels);
                    } else if (progress != download.getProgress()) {
                        progress = download.getProgress();
                        if (modelDownloadingName != null && modelListAdapter.getDataset().stream().anyMatch(it -> it.getName().equals(modelDownloadingName)))
                            modelListAdapter.notifyItemChanged(modelListAdapter.getDataset().indexOf((ModelItem) modelListAdapter.getDataset().stream().filter(it -> it.getName().equals(modelDownloadingName)).findFirst().get()));
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
    }

    private void addOfflineModel(ModelItem modelItem) {
        String offlineListJson = sharedPreferences.getString(PreferenceConstants.OFFLINE_LIST, "[]");
        Gson gson = new Gson();
        List<ModelItem> offlineModels = gson.fromJson(offlineListJson, new TypeToken<List<ModelItem>>() {
        }.getType());
        offlineModels.add(modelItem);
        String offlineModelsJson = gson.toJson(offlineModels);
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
            showWarningDialog(R.string.wait_for_download);
        }
    }

    private void showWarningDialog(int message) {
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.warning))
                .setMessage(message)
                .setPositiveButton("Accept", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private boolean isDownloaded(ModelItem modelItem) {
        return offlineModels.stream().anyMatch(it -> it.getName().equals(modelItem.getName()));
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