package com.ezxuen.contactify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.FieldViewHolder> {

    public interface OnFieldClickListener {
        void onFieldClick(String fieldName);
    }

    private final ArrayList<String> fieldList;
    private final OnFieldClickListener listener;

    public FieldAdapter(ArrayList<String> fieldList, OnFieldClickListener listener) {
        this.fieldList = fieldList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_field, parent, false);
        return new FieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        String field = fieldList.get(position);
        holder.fieldName.setText(field);
        holder.itemView.setOnClickListener(v -> listener.onFieldClick(field));
    }

    @Override
    public int getItemCount() {
        return fieldList.size();
    }

    static class FieldViewHolder extends RecyclerView.ViewHolder {
        TextView fieldName;

        FieldViewHolder(View itemView) {
            super(itemView);
            fieldName = itemView.findViewById(R.id.fieldNameText);
        }
    }
}
