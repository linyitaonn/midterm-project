/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteEditor extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";
    
    private Toolbar mToolbar;

    private static final String[] PROJECTION =
            new String[]{
                    NotePad.Notes._ID, // 0
                    NotePad.Notes.COLUMN_NAME_TITLE, // 1
                    NotePad.Notes.COLUMN_NAME_NOTE, // 2
                    NotePad.Notes.COLUMN_NAME_CATEGORY // 3
            };

    private static final String ORIGINAL_CONTENT = "origContent";

    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;
    private static final int LOADER_ID = 2;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private EditText mTitleText;
    private AutoCompleteTextView mCategoryAutoComplete;
    private String mOriginalContent;
    private boolean mPerformPasteOnLoad = false;

    public static class LinedEditText extends EditText {
        private final Rect mRect;
        private final Paint mPaint;

        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;
            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r);
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) { // 移除了对ACTION_PASTE的处理
            mState = STATE_INSERT;
            Uri insertUri = intent.getData();
            if (insertUri == null) {
                insertUri = NotePad.Notes.CONTENT_URI;
            }
            mUri = getContentResolver().insert(insertUri, null);

            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + insertUri);
                finish();
                return;
            }
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        setContentView(R.layout.note_editor);
        
        // 设置 Toolbar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        
        // 启用返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mTitleText = findViewById(R.id.title);
        mCategoryAutoComplete = findViewById(R.id.category_autocomplete);
        mText = findViewById(R.id.note);

        // 设置分类自动完成文本框
        setupCategoryAutocomplete();

        // 移除了对ACTION_PASTE的特殊处理

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    private void setupCategoryAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, 
                getExistingCategories());
        mCategoryAutoComplete.setAdapter(adapter);
        mCategoryAutoComplete.setThreshold(0); // 输入0个字符就开始显示建议
        
        // 添加焦点监听器，确保点击时显示下拉列表
        mCategoryAutoComplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCategoryAutoComplete.showDropDown();
                }
            }
        });
        
        // 添加点击监听器，确保点击时显示下拉列表
        mCategoryAutoComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCategoryAutoComplete.showDropDown();
            }
        });
        
        // 添加项目选择监听器，选择后清除提示
        mCategoryAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 当用户选择一个项目后，清除提示文本
            }
        });
        
        // 监听文本变化，如果没有文本则恢复提示
        mCategoryAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private List<String> getExistingCategories() {
        Set<String> categories = new HashSet<>();
        Cursor cursor = getContentResolver().query(NotePad.Notes.CONTENT_URI, 
                new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String category = cursor.getString(0);
                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
            }
            cursor.close();
        }
        return new ArrayList<>(categories);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            String text = mText.getText().toString();
            String title = mTitleText.getText().toString();
            String category = mCategoryAutoComplete.getText().toString();

            // 移除了内容为空时删除笔记的逻辑，无论内容是否为空都保存笔记
            updateNote(text, title, category);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }
        
        // 确保在新建笔记状态下也显示保存按钮
        if (mState == STATE_INSERT) {
            menu.findItem(R.id.menu_save).setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mCursor != null && mCursor.moveToFirst()) {
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String savedNote = mCursor.getString(colNoteIndex);
            String currentNote = mText.getText().toString();
            menu.findItem(R.id.menu_revert).setVisible(!savedNote.equals(currentNote));
            
            // 在编辑状态下确保保存按钮可见
            menu.findItem(R.id.menu_save).setVisible(true);
            // 在编辑状态下显示删除按钮
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else if (mState == STATE_INSERT) {
            // 在新建笔记状态下确保保存按钮可见
            menu.findItem(R.id.menu_save).setVisible(true);
            // 在新建笔记状态下隐藏删除按钮
            menu.findItem(R.id.menu_delete).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            String text = mText.getText().toString();
            String title = mTitleText.getText().toString();
            String category = mCategoryAutoComplete.getText().toString();
            updateNote(text, title, category);
            finish();
        } else if (id == R.id.menu_delete) {
            deleteNote();
            finish();
        } else if (id == android.R.id.home) {
            // 处理返回按钮
            handleBackPressed();
            return true;
        } else if (id == R.id.menu_revert) {
            cancelNote();
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleBackPressed() {
        String text = mText.getText().toString();
        String title = mTitleText.getText().toString();
        
        // 如果标题和内容都为空，则直接返回不创建笔记
        if (text.trim().isEmpty() && title.trim().isEmpty()) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            // 如果有内容，则询问用户是否保存
            new AlertDialog.Builder(this)
                    .setTitle("保存笔记")
                    .setMessage("是否保存当前笔记?")
                    .setPositiveButton("保存", (dialog, which) -> {
                        String category = mCategoryAutoComplete.getText().toString();
                        updateNote(text, title, category);
                        finish();
                    })
                    .setNegativeButton("不保存", (dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNeutralButton("取消", null)
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void updateNote(String text, String title, String category) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        if (mState == STATE_INSERT) {
            if (title == null || title.trim().isEmpty()) {
                int length = text.length();
                title = text.substring(0, Math.min(30, length));
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
        }
        if (title != null) {
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        if (category != null) {
            if (!category.trim().isEmpty()) {
                values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, category.trim());
            } else {
                values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "Uncategorized"); // Default category
            }
        }

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        getContentResolver().update(mUri, values, null, null);
    }

    private void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(this, mUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        mCursor = data;

        if (mCursor != null && mCursor.moveToFirst()) {
            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                mTitleText.setText(title);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            int colCategoryIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);
            String category = mCursor.getString(colCategoryIndex);
            if (category != null) {
                mCategoryAutoComplete.setText(category);
            }

            // 每次数据加载完成后刷新分类列表
            setupCategoryAutocomplete();

            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
        
        // 移除了与粘贴相关的代码
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = null;
    }
}
