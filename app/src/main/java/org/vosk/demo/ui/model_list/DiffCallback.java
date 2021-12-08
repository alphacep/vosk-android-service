package org.vosk.demo.ui.model_list;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class DiffCallback extends DiffUtil.ItemCallback<ModelItem> {

    @Override
    public boolean areItemsTheSame(@NonNull ModelItem oldItem, @NonNull ModelItem newItem) {
        return oldItem.getName().equals(newItem.getName());
    }

    @Override
    public boolean areContentsTheSame(@NonNull ModelItem oldItem, @NonNull ModelItem newItem) {
        return oldItem.getName().equals(newItem.getName());
    }
}
