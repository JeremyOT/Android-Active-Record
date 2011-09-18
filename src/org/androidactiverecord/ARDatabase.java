//   Copyright 2011 Jeremy Olmsted-Thompson
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   
package org.androidactiverecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

/**
 * Represents a database to be used by Android Active Record entities.
 * 
 * @author Jeremy Olmsted-Thompson
 */
public class ARDatabase {

    private ARCacheDictionary mDictionary = new ARCacheDictionary();
    private SQLiteDatabase mDatabase;
    private String mPath;
    private Context mContext;
    private boolean mUseExternalStorage = true;

    /**
     * Returns the data path for the application. This is either APPLICATION_PATH or
     * EXTERNAL_STORAGE_DIR/Android/data/PACKAGE_NAME depending on the useExternalStorage argument
     * and the presence of external storage.
     * 
     * @param useExternalStorage
     *            If true, the path will use external storage if available. If false, the path will
     *            be the same as if external storage was missing.
     * @param context
     *            The reference context to use to find the application package name.
     * @return The data path for the application.
     */
    public static String dataPath(boolean useExternalStorage, Context context) {
        return (useExternalStorage
                && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? appendFilePath(
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                String.format("Android%1$sdata%1$s%2$s%1$s", File.separator,
                        context.getPackageName())) : context.getDir(context.getPackageName(), 0)
                .getAbsolutePath());
    }

    /**
     * Creates a new ARDatabase object
     * 
     * @param path
     *            The file name to use for the SQLite database. This can be either a relative or
     *            absolute path. Relative paths will be relative to the application data path. See
     *            dataPath() and setUseExternalStorage() for more information.
     * @param context
     *            The context used for database creation, its package name will be used to place the
     *            database on external storage if any is present, otherwise the context's
     *            application data directory.
     */
    public ARDatabase(String path, Context context) {
        mPath = path;
        mContext = context;
    }

    /**
     * Opens or creates the database file. Uses external storage if present by default, otherwise
     * uses local storage. (See setUseExternalStorage())
     */
    public void open() {
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;
        }
        if (!mPath.startsWith(File.separator)) {
            String dbPath = ARDatabase.dataPath(mUseExternalStorage, mContext);
            new File(dbPath).mkdirs();
            mDatabase = SQLiteDatabase.openDatabase(appendFilePath(dbPath, mPath), null,
                    SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } else {
            new File(mPath.substring(0, mPath.lastIndexOf(File.separator))).mkdirs();
            mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.CREATE_IF_NECESSARY
                    | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }
    }

    public void close() {
        if (mDatabase != null)
            mDatabase.close();
        mDictionary = new ARCacheDictionary();
        mDatabase = null;
    }

    /**
     * Execute some raw SQL.
     * 
     * @param sql
     *            Standard SQLite compatible SQL.
     */
    public void execute(String sql) {
        mDatabase.execSQL(sql);
    }

    /**
     * Insert into a table in the database.
     * 
     * @param table
     *            The table to insert into.
     * @param parameters
     *            The data.
     * @return The ID of the new row.
     */
    public long insert(String table, ContentValues parameters) {
        return mDatabase.insert(table, null, parameters);
    }

    /**
     * Update a table in the database.
     * 
     * @param table
     *            The table to update.
     * @param values
     *            The new values.
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @return The number of rows affected.
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return mDatabase.update(table, values, whereClause, whereArgs);
    }

    /**
     * Delete from a table in the database
     * 
     * @param table
     *            The table to delete from.
     * @param whereClause
     *            The condition to match (Don't include WHERE).
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @return The number of rows affected.
     */
    public int delete(String table, String whereClause, String[] whereArgs) {
        return mDatabase.delete(table, whereClause, whereArgs);
    }

    /**
     * Execute a raw SQL query.
     * 
     * @param sql
     *            Standard SQLite compatible SQL.
     * @return A cursor over the data returned.
     */
    public Cursor rawQuery(String sql) {
        return rawQuery(sql, null);
    }

    /**
     * Execute a raw SQL query.
     * 
     * @param sql
     *            Standard SQLite compatible SQL.
     * @param params
     *            The values to replace "?" with.
     * @return A cursor over the data returned.
     */
    public Cursor rawQuery(String sql, String[] params) {
        return mDatabase.rawQuery(sql, params);
    }

    /**
     * Execute a query.
     * 
     * @param table
     *            The table to query.
     * @param selectColumns
     *            The columns to select.
     * @param where
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @return A cursor over the data returned.
     */
    public Cursor query(String table, String[] selectColumns, String where, String[] whereArgs) {
        return query(false, table, selectColumns, where, whereArgs, null, null, null, null);
    }

    /**
     * Execute a query.
     * 
     * @param distinct
     * @param table
     *            The table to query.
     * @param selectColumns
     *            The columns to select.
     * @param where
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param groupBy
     * @param having
     * @param orderBy
     * @param limit
     * @return A cursor over the data returned.
     */
    public Cursor query(boolean distinct, String table, String[] selectColumns, String where,
            String[] whereArgs, String groupBy, String having, String orderBy, String limit) {
        return mDatabase.query(distinct, table, selectColumns, where, whereArgs, groupBy, having,
                orderBy, limit);
    }

    public String[] getTables() {
        Cursor c = query("sqlite_master", new String[] { "name" }, "type = ?",
                new String[] { "table" });
        List<String> tables = new ArrayList<String>();
        try {
            while (c.moveToNext()) {
                tables.add(c.getString(0));
            }
        } finally {
            c.close();
        }
        return tables.toArray(new String[0]);
    }

    public String[] getColumnsForTable(String table) {
        Cursor c = rawQuery(String.format("PRAGMA table_info(%s)", table));
        List<String> columns = new ArrayList<String>();
        try {
            while (c.moveToNext()) {
                columns.add(c.getString(c.getColumnIndex("name")));
            }
        } finally {
            c.close();
        }
        return columns.toArray(new String[0]);
    }

    public int getVersion() {
        if (!mDatabase.isOpen())
            throw new SQLiteException("Database closed.");
        return mDatabase.getVersion();
    }

    public void setVersion(int version) {
        if (!mDatabase.isOpen())
            throw new SQLiteException("Database closed.");
        mDatabase.setVersion(version);
    }

    /**
     * Set whether the database should try to open on external storage if available. Default is
     * true.
     * 
     * @param useExternalStorage
     */
    public void setUseExternalStorage(boolean useExternalStorage) {
        mUseExternalStorage = useExternalStorage;
    }

    /**
     * Whether or not the database will try to open on external storage if available. Default is
     * true.
     * 
     * @return
     */
    public boolean getUseExternalStorage() {
        return mUseExternalStorage;
    }

    public void beginTransaction() {
        mDatabase.beginTransaction();
    }

    public void setTransactionSuccessful() {
        mDatabase.setTransactionSuccessful();
    }

    public void endTransaction() {
        mDatabase.endTransaction();
    }

    public boolean inTransaction() {
        return mDatabase.inTransaction();
    }

    ARCacheDictionary getEntityCacheDictionary() {
        return mDictionary;
    }

    /**
     * Get the SQLite type for an input class.
     * 
     * @param entityClass
     *            The class to convert.
     * @return A string representing the SQLite type that would be used to store that class.
     */
    protected static String getSQLiteTypeString(Class<?> entityClass) {
        String name = entityClass.getName();
        if (name.equals("java.lang.String"))
            return "text";
        if (name.equals("short"))
            return "int";
        if (name.equals("int"))
            return "int";
        if (name.equals("long"))
            return "int";
        if (name.equals("double"))
            return "real";
        if (name.equals("float"))
            return "real";
        if (name.equals("[B"))
            return "blob";
        if (name.equals("boolean"))
            return "bool";
        if (AREntity.class.isAssignableFrom(entityClass))
            return "int";
        throw new IllegalArgumentException("Class cannot be stored in Sqlite3 database.");
    }

    /**
     * Append to a file path, takes extra or missing separator characters into account.
     * 
     * @param path
     *            The root path.
     * @param append
     *            What to add.
     * @return The new path.
     */
    private static String appendFilePath(String path, String append) {
        return path
                .concat(path.endsWith(File.separator) ? (append.startsWith(File.separator) ? append
                        .substring(1) : append) : File.separator.concat((append
                        .startsWith(File.separator) ? append.substring(1) : append)));
    }
}
