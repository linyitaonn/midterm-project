package com.example.android.notepad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_NOTE = 1;

    private final Context mContext;
    private final List<Object> mItems;
    private final OnNoteListener mOnNoteListener;

    public interface OnNoteListener {
        void onNoteClick(NotesListFragment.NoteHolder note);
        void onDeleteClick(NotesListFragment.NoteHolder note);
    }

    public NoteCategoryAdapter(Context context, List<Object> items, OnNoteListener onNoteListener) {
        mContext = context;
        mItems = items;
        mOnNoteListener = onNoteListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof String) {
            return TYPE_CATEGORY;
        } else {
            return TYPE_NOTE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CATEGORY) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_note, parent, false);
            return new NoteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_CATEGORY) {
            CategoryViewHolder categoryViewHolder = (CategoryViewHolder) holder;
            categoryViewHolder.bind((String) mItems.get(position));
        } else {
            NoteViewHolder noteViewHolder = (NoteViewHolder) holder;
            noteViewHolder.bind((NotesListFragment.NoteHolder) mItems.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView mCategoryTitle;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            mCategoryTitle = itemView.findViewById(R.id.category_title);
        }

        void bind(String category) {
            mCategoryTitle.setText(category);
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView mNoteTitle;
        private final TextView mNoteTimestamp;
        private final ImageButton mDeleteButton;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            mNoteTitle = itemView.findViewById(R.id.note_title);
            mNoteTimestamp = itemView.findViewById(R.id.note_timestamp);
            mDeleteButton = itemView.findViewById(R.id.delete_button);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    mOnNoteListener.onNoteClick((NotesListFragment.NoteHolder) mItems.get(pos));
                }
            });

            mDeleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    mOnNoteListener.onDeleteClick((NotesListFragment.NoteHolder) mItems.get(pos));
                }
            });
        }

        void bind(NotesListFragment.NoteHolder note) {
            mNoteTitle.setText(note.title);
            
            // 格式化并显示时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(note.modificationDate));
            mNoteTimestamp.setText(formattedDate);
        }
    }
}