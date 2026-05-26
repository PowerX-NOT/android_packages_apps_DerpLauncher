/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

import java.util.List;
import java.util.function.Consumer;

class HiddenAppsDrawerAdapter extends RecyclerView.Adapter<HiddenAppsDrawerAdapter.Holder> {

    private final List<HiddenAppsDrawerActivity.HiddenAppEntry> mEntries;
    private final Consumer<HiddenAppsDrawerActivity.HiddenAppEntry> mOnClick;

    HiddenAppsDrawerAdapter(List<HiddenAppsDrawerActivity.HiddenAppEntry> entries,
            Consumer<HiddenAppsDrawerActivity.HiddenAppEntry> onClick) {
        mEntries = entries;
        mOnClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.hidden_apps_drawer_item, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HiddenAppsDrawerActivity.HiddenAppEntry entry = mEntries.get(position);
        holder.title.setText(entry.label);
        holder.icon.setImageDrawable(entry.icon);
        holder.itemView.setOnClickListener(v -> mOnClick.accept(entry));
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.hidden_app_icon);
            title = itemView.findViewById(R.id.hidden_app_title);
        }
    }
}
