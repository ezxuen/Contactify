package com.ezxuen.contactify;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupedContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> displayList;
    private final OnContactClickListener contactClickListener;

    public interface OnContactClickListener {
        void onContactClick(int contactId);
    }

    public GroupedContactAdapter(LinkedHashMap<String, ArrayList<Pair<Integer, String>>> groupedData,
                                 OnContactClickListener listener) {
        displayList = new ArrayList<>();
        contactClickListener = listener;

        for (Map.Entry<String, ArrayList<Pair<Integer, String>>> entry : groupedData.entrySet()) {
            String jobTitle = entry.getKey();
            ArrayList<Pair<Integer, String>> contacts = entry.getValue();

            displayList.add(jobTitle); // Header
            displayList.addAll(contacts); // Contact items
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (displayList.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_contact, parent, false);
            return new ContactViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            String jobTitle = (String) displayList.get(position);
            ((HeaderViewHolder) holder).headerText.setText(jobTitle);
        } else {
            Pair<Integer, String> contact = (Pair<Integer, String>) displayList.get(position);
            ((ContactViewHolder) holder).nameText.setText(contact.second);

            holder.itemView.setOnClickListener(v -> {
                if (contactClickListener != null) {
                    contactClickListener.onContactClick(contact.first);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;

        public ContactViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
        }
    }
}