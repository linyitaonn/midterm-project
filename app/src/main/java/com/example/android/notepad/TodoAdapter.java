package com.example.android.notepad;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    public TodoAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.todo_item, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            return;
        }

        long id = mCursor.getLong(mCursor.getColumnIndex(NotePad.Todos._ID));
        String text = mCursor.getString(mCursor.getColumnIndex(NotePad.Todos.COLUMN_NAME_TEXT));
        String color = mCursor.getString(mCursor.getColumnIndex(NotePad.Todos.COLUMN_NAME_COLOR));
        boolean isCompleted = mCursor.getInt(mCursor.getColumnIndex(NotePad.Todos.COLUMN_NAME_IS_COMPLETED)) == 1;

        holder.mTextView.setText(text);
        holder.mCardView.setCardBackgroundColor(Color.parseColor(color));
        // 不再设置CheckBox的状态，因为我们移除了CheckBox

        if (isCompleted) {
            holder.mTextView.setPaintFlags(holder.mTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.mCardView.setCardBackgroundColor(Color.LTGRAY);
        } else {
            holder.mTextView.setPaintFlags(holder.mTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.itemView.setTag(id);
        holder.itemView.setOnClickListener(v -> {
            boolean newCompletedState = !isCompleted;
            ContentValues values = new ContentValues();
            values.put(NotePad.Todos.COLUMN_NAME_IS_COMPLETED, newCompletedState);
            mContext.getContentResolver().update(NotePad.Todos.CONTENT_URI, values, NotePad.Todos._ID + "=?", new String[]{String.valueOf(id)});
            
            // 更新UI状态
            if (newCompletedState) {
                holder.mTextView.setPaintFlags(holder.mTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.mCardView.setCardBackgroundColor(Color.LTGRAY);
            } else {
                holder.mTextView.setPaintFlags(holder.mTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.mCardView.setCardBackgroundColor(Color.parseColor(color));
            }
        });
    }

    @Override
    public int getItemCount() {
        return (mCursor == null) ? 0 : mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = newCursor;
        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public CardView mCardView;
        // 移除了mCheckBox字段，因为我们不再使用CheckBox

        public TodoViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.todo_text);
            mCardView = itemView.findViewById(R.id.todo_card);
            // 不再查找CheckBox，因为我们移除了它
        }
    }
}