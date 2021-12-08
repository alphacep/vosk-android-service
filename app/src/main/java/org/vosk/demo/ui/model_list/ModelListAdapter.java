package org.vosk.demo.ui.model_list;

import static org.vosk.demo.api.Download.CLEAR;
import static org.vosk.demo.api.Download.COMPLETE;
import static org.vosk.demo.api.Download.RESTARTING;
import static org.vosk.demo.api.Download.STARTING;
import static org.vosk.demo.api.Download.UNZIPPING;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.vosk.demo.R;
import org.vosk.demo.utils.EventBus;
import org.vosk.demo.utils.PreferenceConstants;

import java.util.ArrayList;
import java.util.List;

public class ModelListAdapter extends ListAdapter<ModelItem, ModelListAdapter.ViewHolder> {

    List<ModelItem> dataset = new ArrayList<>();
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
        submitList(newDataset);
    }


    @Override
    public int getItemCount() {
        return dataset.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView modelName;
        TextView downloadProgress;
        ImageView downloadedIndicator;

        ViewHolder(View v) {
            super(v);
            modelName = v.findViewById(R.id.model_name);
            downloadProgress = v.findViewById(R.id.model_download_progress);
            downloadedIndicator = v.findViewById(R.id.downloaded_indicator);
        }

        public void bind(ModelItem modelItem) {
            modelName.setText(modelItem.getName());
            itemView.setOnClickListener(v -> EventBus.getInstance().postModelSelectedObservable(getCurrentList().get(getAdapterPosition())));

            int dotColor;
            if (sharedPreferences.getString(PreferenceConstants.ACTIVE_MODEL, "").equals(dataset.get(getAdapterPosition()).getName())) {
                dotColor = R.color.indicator_green;
            } else {
                dotColor = R.color.indicator_red;
            }
            ColorStateList csl = AppCompatResources.getColorStateList(itemView.getContext(), dotColor);
            ImageViewCompat.setImageTintList(downloadedIndicator, csl);

            if (getAdapterPosition() != -1 && sharedPreferences.contains(PreferenceConstants.DOWNLOADING_FILE) && sharedPreferences.getString(PreferenceConstants.DOWNLOADING_FILE, "").equals(dataset.get(getAdapterPosition()).getName())) {
                downloadProgress.setVisibility(View.VISIBLE);
                switch (ModelListActivity.progress) {
                    case COMPLETE:
                    case CLEAR:
                        downloadProgress.setVisibility(View.GONE);
                        break;
                    case STARTING:
                        downloadProgress.setText(R.string.model_download_start);
                        break;
                    case RESTARTING:
                        downloadProgress.setText(R.string.model_download_restart);
                        break;
                    case UNZIPPING:
                        downloadProgress.setText(R.string.model_download_unzipping);
                        break;
                    default: {
                        String progress = "Downloading: " + ModelListActivity.progress + "%";
                        if (!downloadProgress.getText().equals(progress))
                            downloadProgress.setText(progress);
                    }
                }
            } else downloadProgress.setVisibility(View.GONE);

        }
    }


    public List<ModelItem> getDataset() {
        return dataset;
    }
}
