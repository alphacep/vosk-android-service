package org.vosk.demo.ui.model_list;

import static org.vosk.demo.api.Download.COMPLETE;
import static org.vosk.demo.api.Download.UNZIPPING;
import static org.vosk.demo.ui.model_list.ModelListActivity.progress;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.Group;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.vosk.demo.R;
import org.vosk.demo.api.Download;
import org.vosk.demo.utils.EventBus;
import org.vosk.demo.utils.PreferenceConstants;

import java.util.ArrayList;
import java.util.List;

public class ModelListAdapter extends ListAdapter<ModelItem, ModelListAdapter.ViewHolder> {

    List<ModelItem> dataset = new ArrayList<>();
    List<ModelItem> offlineModelItems = new ArrayList<>();
    SharedPreferences sharedPreferences;

    public ModelListAdapter(SharedPreferences sharedPreferences) {
        super(new DiffCallback());
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.model_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(dataset.get(position));
    }

    public void updateDataset(List<ModelItem> newDataset) {

        dataset.clear();
        dataset.addAll(newDataset);

        this.offlineModelItems.clear();
        this.offlineModelItems.addAll(newDataset);

        submitList(newDataset);
    }

    public void updateDataset(List<ModelItem> newDataset, List<ModelItem> offlineModelItems) {

        dataset.clear();
        dataset.addAll(newDataset);

        this.offlineModelItems.clear();
        this.offlineModelItems.addAll(offlineModelItems);

        submitList(newDataset);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public void updateOfflineModels(List<ModelItem> offlineModels) {
        this.offlineModelItems.clear();
        this.offlineModelItems.addAll(offlineModels);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView modelLangText;
        TextView modelSize;
        TextView modelName;
        TextView downloadProgressText;
        ProgressBar downloadProgressBar;
        ImageView modelIndicator;
        Group downloadProgressGroup;

        ViewHolder(View v) {
            super(v);
            modelLangText = v.findViewById(R.id.model_lang_text);
            modelSize = v.findViewById(R.id.model_size);
            modelName = v.findViewById(R.id.model_name);
            downloadProgressText = v.findViewById(R.id.model_download_progress_text);
            downloadProgressBar = v.findViewById(R.id.model_download_progress_bar);
            modelIndicator = v.findViewById(R.id.model_indicator);
            downloadProgressGroup = v.findViewById(R.id.model_download_progress_group);
        }

        public void bind(ModelItem modelItem) {
            ModelListState modelListState = getModelListState();

            modelLangText.setText(modelItem.getLang_text());
            modelSize.setText(itemView.getContext().getString(R.string.model_size, modelItem.getSize_text()));
            modelName.setText(itemView.getContext().getString(R.string.model_name, modelItem.getName()));
            View.OnClickListener sendSelectedEvent = v -> EventBus.getInstance().postModelSelectedObservable(getCurrentList().get(getAdapterPosition()));
            itemView.setOnClickListener(sendSelectedEvent);
            modelIndicator.setOnClickListener(sendSelectedEvent);
            itemView.setOnLongClickListener(v -> {
                if (modelListState == ModelListState.DOWNLOADED)
                    EventBus.getInstance().postDeleteDownloadedModel(getCurrentList().get(getAdapterPosition()));
                return true;
            });

            switch (modelListState) {

                case NOT_DOWNLOADED:
                    downloadProgressGroup.setVisibility(View.GONE);
                    setDownloadProgress(0);
                    modelIndicator.setVisibility(View.VISIBLE);
                    setIndicator(R.drawable.ic_baseline_cloud_download_24);
                    break;
                case DOWNLOADED:
                    downloadProgressGroup.setVisibility(View.GONE);
                    setDownloadProgress(0);
                    modelIndicator.setVisibility(View.GONE);
                    break;
                case DOWNLOADING:
                    downloadProgressGroup.setVisibility(View.VISIBLE);
                    setDownloadProgress(progress);
                    modelIndicator.setVisibility(View.GONE);
                    break;
                case SELECTED:
                    downloadProgressGroup.setVisibility(View.GONE);
                    modelIndicator.setVisibility(View.VISIBLE);
                    setDownloadProgress(0);
                    setIndicator(R.drawable.ic_baseline_check_circle_24);
                    break;
            }
        }

        private void setDownloadProgress(int progress) {
            if (progress <= 100) {
                downloadProgressBar.setProgress(progress);
                downloadProgressText.setText(itemView.getContext().getString(R.string.model_downloading_progress, progress));
            }
        }

        private void setIndicator(int indicatorIcon) {
            ColorStateList csl;
            if (indicatorIcon == R.drawable.ic_baseline_check_circle_24)
                csl = AppCompatResources.getColorStateList(itemView.getContext(), R.color.indicator_green);
            else
                csl = AppCompatResources.getColorStateList(itemView.getContext(), R.color.indicator_gray);

            modelIndicator.setBackgroundTintList(csl);
            modelIndicator.setBackground(AppCompatResources.getDrawable(itemView.getContext(), indicatorIcon));
        }

        public ModelListState getModelListState() {
            if (getAdapterPosition() != -1 && sharedPreferences.contains(PreferenceConstants.DOWNLOADING_FILE) && sharedPreferences.getString(PreferenceConstants.DOWNLOADING_FILE, "").equals(dataset.get(getAdapterPosition()).getName()))
                return ModelListState.DOWNLOADING;
            if (sharedPreferences.getString(PreferenceConstants.ACTIVE_MODEL, "").equals(dataset.get(getAdapterPosition()).getName()))
                return ModelListState.SELECTED;
            if (offlineModelItems.stream().anyMatch(it -> it.getName().equals(dataset.get(getAdapterPosition()).getName()))) {
                return ModelListState.DOWNLOADED;
            }
            return ModelListState.NOT_DOWNLOADED;
        }
    }


    public List<ModelItem> getDataset() {
        return dataset;
    }

    enum ModelListState {
        NOT_DOWNLOADED,
        DOWNLOADED,
        DOWNLOADING,
        SELECTED
    }
}
