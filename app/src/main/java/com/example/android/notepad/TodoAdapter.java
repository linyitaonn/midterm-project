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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
        
        // 长按项目触发删除操作
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteConfirmationDialog(id, text, holder.itemView);
            return true;
        });
    }

    private void showDeleteConfirmationDialog(long id, String text, View itemView) {
        new AlertDialog.Builder(mContext)
                .setTitle("删除任务")
                .setMessage("确定要删除任务 \"" + text + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 执行删除操作
                    int rowsDeleted = mContext.getContentResolver().delete(
                            NotePad.Todos.CONTENT_URI,
                            NotePad.Todos._ID + "=?",
                            new String[]{String.valueOf(id)}
                    );
                    
                    if (rowsDeleted > 0) {
                        // 显示删除成功提示
                        Toast.makeText(mContext, "任务已删除", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
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

        public TodoViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.todo_text);
            mCardView = itemView.findViewById(R.id.todo_card);
        }
    }
}