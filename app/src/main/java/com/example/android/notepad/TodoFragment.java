package com.example.android.notepad;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TodoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 1;
    private TodoAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar_todos);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAdapter = new TodoAdapter(getContext(), null);
        recyclerView.setAdapter(mAdapter);

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_todo);
        fab.setOnClickListener(v -> showAddTodoDialog());

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                long id = (long) viewHolder.itemView.getTag();
                getContext().getContentResolver().delete(NotePad.Todos.CONTENT_URI, NotePad.Todos._ID + "=?", new String[]{String.valueOf(id)});
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void showAddTodoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_todo, null);
        builder.setView(dialogView);

        final EditText todoText = dialogView.findViewById(R.id.edit_text_todo);
        final View colorPreview = dialogView.findViewById(R.id.color_preview);

        final String[] selectedColor = {"#FFFFFF"}; // Default color

        // 使用更高级的颜色
        dialogView.findViewById(R.id.color_coral).setOnClickListener(v -> {
            selectedColor[0] = "#FF7F7F"; // 柔和的珊瑚色
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_mint).setOnClickListener(v -> {
            selectedColor[0] = "#7FFFD4"; // 薄荷绿
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_lavender).setOnClickListener(v -> {
            selectedColor[0] = "#E6E6FA"; // 淡紫色
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        
        // 新增的高贵颜色
        dialogView.findViewById(R.id.color_rose_gold).setOnClickListener(v -> {
            selectedColor[0] = "#B76E79"; // 玫瑰金
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_sapphire).setOnClickListener(v -> {
            selectedColor[0] = "#0F52BA"; // 宝石蓝
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_emerald).setOnClickListener(v -> {
            selectedColor[0] = "#50C878"; // 祖母绿
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_amethyst).setOnClickListener(v -> {
            selectedColor[0] = "#9966CC"; // 紫水晶
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });
        dialogView.findViewById(R.id.color_gold).setOnClickListener(v -> {
            selectedColor[0] = "#FFD700"; // 黄金
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
            dialogView.setBackgroundColor(Color.parseColor(selectedColor[0]));
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String text = todoText.getText().toString();
            if (!text.isEmpty()) {
                ContentValues values = new ContentValues();
                values.put(NotePad.Todos.COLUMN_NAME_TEXT, text);
                values.put(NotePad.Todos.COLUMN_NAME_COLOR, selectedColor[0]);
                getContext().getContentResolver().insert(NotePad.Todos.CONTENT_URI, values);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(getContext(), NotePad.Todos.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
