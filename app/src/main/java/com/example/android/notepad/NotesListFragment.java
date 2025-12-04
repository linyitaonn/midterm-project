package com.example.android.notepad;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotesListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, NoteCategoryAdapter.OnNoteListener {

    private static final String TAG = "NotesListFragment";

    private static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
            NotePad.Notes.COLUMN_NAME_CATEGORY // 3
    };

    private static final int LOADER_ID = 0;

    private NoteCategoryAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Toolbar mToolbar;

    public static class NoteHolder {
        long id;
        String title;
        long modificationDate;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = view.findViewById(R.id.toolbar);
        if (getActivity() != null) {
            if (getActivity() instanceof AppCompatActivity) {
                ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
            }
        }

        mRecyclerView = view.findViewById(R.id.notes_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置 FloatingActionButton
        FloatingActionButton fab = view.findViewById(R.id.fab_add_note);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI));
        });

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (mToolbar != null) {
            mToolbar.inflateMenu(R.menu.list_options_menu);
            Menu toolbarMenu = mToolbar.getMenu();

            MenuItem searchItem = toolbarMenu.findItem(R.id.menu_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        performSearch(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (newText.isEmpty()) {
                            performSearch(null);
                        }
                        return true;
                    }
                });
            }

            MenuItem addItem = toolbarMenu.findItem(R.id.menu_add);
            addItem.setOnMenuItemClickListener(item -> {
                startActivity(new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI));
                return true;
            });
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void performSearch(String query) {
        Bundle args = new Bundle();
        if (query != null && !query.isEmpty()) {
            args.putString("query", query);
        }
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, args, this);
    }

    private void filterByCategory(String category) {
        Bundle args = new Bundle();
        if (category != null && !category.equals("Show All")) {
            args.putString("category", category);
        }
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, args, this);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (getActivity() != null) {
            SubMenu categorySubMenu = menu.findItem(R.id.menu_filter_category).getSubMenu();
            categorySubMenu.clear();
            categorySubMenu.add(Menu.NONE, R.id.menu_show_all_categories, Menu.NONE, "Show All");

            Set<String> categories = new HashSet<>();
            Cursor categoryCursor = getActivity().getContentResolver().query(NotePad.Notes.CONTENT_URI,
                    new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY}, null, null, null);

            if (categoryCursor != null) {
                while (categoryCursor.moveToNext()) {
                    String category = categoryCursor.getString(0);
                    if (category != null && !category.isEmpty()) {
                        categories.add(category);
                    }
                }
                categoryCursor.close();
            }

            List<String> sortedCategories = new ArrayList<>(categories);
            Collections.sort(sortedCategories);

            for (String category : sortedCategories) {
                categorySubMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, category).setCheckable(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (getActivity() != null) {
            int id = item.getItemId();
            if (id == R.id.menu_show_all_categories) {
                filterByCategory(null);
                return true;
            } else if (item.getGroupId() == Menu.NONE && id != R.id.menu_search && id != R.id.menu_filter_category) { // Dynamic category items
                filterByCategory(item.getTitle().toString());
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        Uri baseUri = NotePad.Notes.CONTENT_URI;
        String selection = null;
        String[] selectionArgs = null;

        if (args != null) {
            String query = args.getString("query");
            String category = args.getString("category");

            if (query != null && !query.isEmpty()) {
                selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
            } else if (category != null && !category.isEmpty()) {
                selection = NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
                selectionArgs = new String[]{category};
            }
        }

        return new CursorLoader(requireActivity(), baseUri,
                PROJECTION, selection, selectionArgs,
                NotePad.Notes.COLUMN_NAME_CATEGORY + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        Map<String, List<NoteHolder>> categoryMap = new HashMap<>();

        if (data != null) {
            int idCol = data.getColumnIndexOrThrow(NotePad.Notes._ID);
            int titleCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE);
            int modDateCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
            int categoryCol = data.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_CATEGORY);

            while (data.moveToNext()) {
                String category = data.getString(categoryCol);
                if (category == null || category.trim().isEmpty()) {
                    category = "Uncategorized";
                }

                if (!categoryMap.containsKey(category)) {
                    categoryMap.put(category, new ArrayList<>());
                }

                NoteHolder note = new NoteHolder();
                note.id = data.getLong(idCol);
                note.title = data.getString(titleCol);
                note.modificationDate = data.getLong(modDateCol);

                categoryMap.get(category).add(note);
            }
        }

        List<String> sortedCategories = new ArrayList<>(categoryMap.keySet());
        Collections.sort(sortedCategories);

        List<Object> items = new ArrayList<>();
        for (String category : sortedCategories) {
            items.add(category);
            items.addAll(categoryMap.get(category));
        }

        mAdapter = new NoteCategoryAdapter(getContext(), items, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onNoteClick(NoteHolder note) {
        if (getActivity() != null) {
            Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, note.id);
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

    @Override
    public void onDeleteClick(NoteHolder note) {
        showDeleteConfirmationDialog(note);
    }

    private void showDeleteConfirmationDialog(NoteHolder note) {
        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, note.id);
                    if (getActivity() != null) {
                        getActivity().getContentResolver().delete(noteUri, null, null);
                        // The loader will automatically update the list.
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }
}
