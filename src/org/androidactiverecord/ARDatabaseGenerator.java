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

import java.lang.reflect.Field;

/**
 * Generates and updates ARDatabases.
 * 
 * @author Jeremy Olmsted-Thompson
 */
public class ARDatabaseGenerator {
    private ARDatabase mDatabase;
    private boolean mMigrating = false;
    private boolean mUpdating = false;
    private static int mDatabaseVersion;

    /**
     * Create a new ARDatabaseGenerator for a database.
     * 
     * @param database
     *            The database to generate or upgrade.
     * @param currentVersion
     *            The version that an up to date database would have.
     */
    public ARDatabaseGenerator(ARDatabase database, int currentVersion) {
        mDatabase = database;
        mDatabaseVersion = currentVersion;
    }

    public boolean needsUpdate() {
        return mDatabase.getVersion() != mDatabaseVersion;
    }

    /**
     * Add or update a table for an AREntity that is stored in the current database.
     * 
     * @param <T>
     *            Any AREntity type.
     * @param newClass
     *            The class to reference when updating or adding a table.
     * @throws ARInflationException
     * @throws IllegalStateException
     */
    public <T extends AREntity> void addClass(Class<T> newClass) {
        if (!mUpdating)
            throw new IllegalStateException("Cannot modify database before initializing update.");
        boolean createTable = true;
        T entity;

        try {
            entity = newClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        }
        String name = entity.getTableName();
        if (mMigrating) {
            String[] tables = mDatabase.getTables();
            for (int i = 0; i < tables.length; i++) {
                if (tables[i].equals(name)) {
                    createTable = false;
                    break;
                }
            }
        }
        if (createTable) {
            StringBuilder sb = new StringBuilder("CREATE TABLE ").append(name).append(
                            " (_id integer primary key");
            for (Field column : entity.getColumnFieldsWithoutID()) {
                sb.append(", ").append(column.getName()).append(" ")
                                .append(ARDatabase.getSQLiteTypeString(column.getType()));
            }
            sb.append(")");
            mDatabase.execute(sb.toString());
        } else {
            String[] existingColumns = mDatabase.getColumnsForTable(name);
            for (Field column : entity.getColumnFieldsWithoutID()) {
                boolean addColumn = true;
                for (int j = 0; j < existingColumns.length; j++) {
                    if (existingColumns[j].equals(column.getName())) {
                        addColumn = false;
                        break;
                    }
                }
                if (addColumn) {
                    mDatabase.execute(String.format("ALTER TABLE %s ADD COLUMN %s %s", name,
                                    column.getName(),
                                    ARDatabase.getSQLiteTypeString(column.getType())));
                }
            }
        }
    }

    /**
     * Call this before calling addClass(), will wipe the existing database.
     */
    public void beginUpdate() {
        beginUpdate(true);
    }

    /**
     * Call this before calling addClass().
     * 
     * @param clear
     *            Whether or not to drop all existing data. Use this if breaking changes have been
     *            made.
     */
    public void beginUpdate(boolean clear) {
        String[] tables = mDatabase.getTables();
        if (clear) {
            for (String table : tables)
                mDatabase.execute(String.format("drop table %s", table));
        }
        if (!clear && tables.length > 0)
            mMigrating = true;
        mUpdating = true;
    }

    /**
     * Call this after all updates have been completed, when there will be no more calls to
     * addClass().
     * 
     * @throws Exception
     */
    public void endUpdate() {
        mUpdating = false;
        mMigrating = false;
        mDatabase.setVersion(mDatabaseVersion);
    }
}
