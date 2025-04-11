package com.ezxuen.contactify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class IndustryAdapter extends RecyclerView.Adapter<IndustryAdapter.ViewHolder> {

    public interface OnIndustryClickListener {
        void onIndustryClick(String industryName);
    }

    private final ArrayList<String> industries;
    private final OnIndustryClickListener listener;

    public IndustryAdapter(ArrayList<String> industries, OnIndustryClickListener listener) {
        this.industries = industries;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.itemText);
        }
    }

    @NonNull
    @Override
    public IndustryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IndustryAdapter.ViewHolder holder, int position) {
        String industry = industries.get(position);
        holder.title.setText(industry);
        holder.itemView.setOnClickListener(v -> listener.onIndustryClick(industry));
    }

    @Override
    public int getItemCount() {
        return industries.size();
    }
}