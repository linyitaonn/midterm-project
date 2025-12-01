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

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class NotePadProvider extends ContentProvider implements ContentProvider.PipeDataWriter<Cursor> {
    // Used for debugging and logging
    private static final String TAG = "NotePadProvider";

    private static final String DATABASE_NAME = "note_pad.db";
    private static final int DATABASE_VERSION = 4;

    private static HashMap<String, String> sNotesProjectionMap;
    private static HashMap<String, String> sTodosProjectionMap;
    private static HashMap<String, String> sLiveFolderProjectionMap;

    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final int LIVE_FOLDER_NOTES = 3;
    private static final int TODOS = 4;
    private static final int TODO_ID = 5;

    private static final UriMatcher sUriMatcher;

    private DatabaseHelper mOpenHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "todos", TODOS);
        sUriMatcher.addURI(NotePad.AUTHORITY, "todos/#", TODO_ID);

        sNotesProjectionMap = new HashMap<>();
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, NotePad.Notes.COLUMN_NAME_CREATE_DATE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.COLUMN_NAME_CATEGORY);

        sTodosProjectionMap = new HashMap<>();
        sTodosProjectionMap.put(NotePad.Todos._ID, NotePad.Todos._ID);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_TEXT, NotePad.Todos.COLUMN_NAME_TEXT);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_COLOR, NotePad.Todos.COLUMN_NAME_COLOR);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_IS_COMPLETED, NotePad.Todos.COLUMN_NAME_IS_COMPLETED);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_CREATE_DATE, NotePad.Todos.COLUMN_NAME_CREATE_DATE);
        sTodosProjectionMap.put(NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE, NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE);

        sLiveFolderProjectionMap = new HashMap<>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " + LiveFolders.NAME);
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT"
                    + ");");

            db.execSQL("CREATE TABLE " + NotePad.Todos.TABLE_NAME + " ("
                    + NotePad.Todos._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Todos.COLUMN_NAME_TEXT + " TEXT,"
                    + NotePad.Todos.COLUMN_NAME_COLOR + " TEXT,"
                    + NotePad.Todos.COLUMN_NAME_IS_COMPLETED + " INTEGER DEFAULT 0,"
                    + NotePad.Todos.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ".");
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME + " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT;");
            }
            if (oldVersion < 4) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + NotePad.Todos.TABLE_NAME + " ("
                        + NotePad.Todos._ID + " INTEGER PRIMARY KEY,"
                        + NotePad.Todos.COLUMN_NAME_TEXT + " TEXT,"
                        + NotePad.Todos.COLUMN_NAME_COLOR + " TEXT,"
                        + NotePad.Todos.COLUMN_NAME_IS_COMPLETED + " INTEGER DEFAULT 0,"
                        + NotePad.Todos.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                        + NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                        + ");");
            }
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                break;
            case NOTE_ID:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(NotePad.Notes._ID + "=" + uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;
            case LIVE_FOLDER_NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;
            case TODOS:
                qb.setTables(NotePad.Todos.TABLE_NAME);
                qb.setProjectionMap(sTodosProjectionMap);
                break;
            case TODO_ID:
                qb.setTables(NotePad.Todos.TABLE_NAME);
                qb.setProjectionMap(sTodosProjectionMap);
                qb.appendWhere(NotePad.Todos._ID + "=" + uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        if (sUriMatcher.match(uri) == TODOS || sUriMatcher.match(uri) == TODO_ID) {
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = NotePad.Todos.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
        } else {
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return NotePad.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;
            case TODOS:
                return NotePad.Todos.CONTENT_TYPE;
            case TODO_ID:
                return NotePad.Todos.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        if (sUriMatcher.match(uri) == NOTE_ID) {
            return super.getStreamTypes(uri, mimeTypeFilter);
        }
        return null;
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        if (sUriMatcher.match(uri) == NOTE_ID) {
            return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
        }
        return null;
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, Cursor c) {
        // Default implementation
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = -1;
        Uri noteUri = null;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
                    Resources r = Resources.getSystem();
                    values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY)) {
                    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, "Uncategorized");
                }

                rowId = db.insert(NotePad.Notes.TABLE_NAME, NotePad.Notes.COLUMN_NAME_NOTE, values);
                if (rowId > 0) {
                    noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            case TODOS:
                if (!values.containsKey(NotePad.Todos.COLUMN_NAME_CREATE_DATE)) {
                    values.put(NotePad.Todos.COLUMN_NAME_CREATE_DATE, now);
                }
                if (!values.containsKey(NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE)) {
                    values.put(NotePad.Todos.COLUMN_NAME_MODIFICATION_DATE, now);
                }
                if (!values.containsKey(NotePad.Todos.COLUMN_NAME_TEXT)) {
                    values.put(NotePad.Todos.COLUMN_NAME_TEXT, "");
                }
                if (!values.containsKey(NotePad.Todos.COLUMN_NAME_COLOR)) {
                    values.put(NotePad.Todos.COLUMN_NAME_COLOR, "#FFFFFF"); // Default white color
                }
                rowId = db.insert(NotePad.Todos.TABLE_NAME, null, values);
                if (rowId > 0) {
                    noteUri = ContentUris.withAppendedId(NotePad.Todos.CONTENT_ID_URI_BASE, rowId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowId > 0) {
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(NotePad.Notes.TABLE_NAME, where, whereArgs);
                break;
            case NOTE_ID:
                finalWhere = NotePad.Notes._ID + " = " + uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
                count = db.delete(NotePad.Notes.TABLE_NAME, finalWhere, whereArgs);
                break;
            case TODOS:
                count = db.delete(NotePad.Todos.TABLE_NAME, where, whereArgs);
                break;
            case TODO_ID:
                finalWhere = NotePad.Todos._ID + " = " + uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION);
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
                count = db.delete(NotePad.Todos.TABLE_NAME, finalWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(NotePad.Notes.TABLE_NAME, values, where, whereArgs);
                break;
            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
                finalWhere = NotePad.Notes._ID + " = " + noteId;
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
                count = db.update(NotePad.Notes.TABLE_NAME, values, finalWhere, whereArgs);
                break;
            case TODOS:
                count = db.update(NotePad.Todos.TABLE_NAME, values, where, whereArgs);
                break;
            case TODO_ID:
                String todoId = uri.getPathSegments().get(NotePad.Todos.TODO_ID_PATH_POSITION);
                finalWhere = NotePad.Todos._ID + " = " + todoId;
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }
                count = db.update(NotePad.Todos.TABLE_NAME, values, finalWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}
