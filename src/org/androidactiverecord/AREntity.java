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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * An entity persisted in a SQLite database.
 * 
 * @author Jeremy Olmsted-Thompson
 */
public class AREntity {

    protected volatile long _id = 0;
    protected volatile boolean m_NeedsInsert = true;
    protected volatile boolean m_InflationRequired = false;
    protected ARDatabase m_Database = null;

    /**
     * This entity's row id.
     * 
     * @return The SQLite row id.
     */
    public long getId() {
        return _id;
    }

    /**
     * Set this entity's id.
     * 
     * @param id
     *            The new id.
     */
    protected void setId(long id) {
        _id = id;
    }

    /**
     * Get the table name for this class.
     * 
     * @return The table name for this class.
     */
    protected String getTableName() {
        return getClass().getSimpleName();
    }

    /**
     * Get this class's columns without the id column.
     * 
     * @return An array of the columns in this class's table.
     */
    protected String[] getColumnsWithoutID() {
        List<String> columns = new ArrayList<String>();
        for (Field field : getColumnFieldsWithoutID()) {
            columns.add(field.getName());
        }
        return columns.toArray(new String[0]);
    }

    /**
     * Get this class's column names, excluding those marked with ARDelayedInflation.
     * 
     * @return An array of the columns in this class's table.
     */
    protected String[] getColumns() {
        return getColumns(false);
    }

    /**
     * Get this class's columns.
     * 
     * @param includeDelayed
     *            Whether or not to include columns marked with ARDelayedInflation.
     * 
     * @return An array of the columns in this class's table.
     */
    protected String[] getColumns(boolean includeDelayed) {
        List<String> columns = new ArrayList<String>();
        for (Field field : getColumnFields()) {
            if (includeDelayed || !field.isAnnotationPresent(ARDelayedInflation.class))
                columns.add(field.getName());
        }
        return columns.toArray(new String[0]);
    }

    /**
     * Get this class's fields without _id.
     * 
     * @return An array of fields for this class.
     */
    protected List<Field> getColumnFieldsWithoutID() {
        Field[] fields = getClass().getDeclaredFields();
        Set<Field> columns = new HashSet<Field>();
        for (Field field : fields) {
            if (!field.getName().startsWith("m_") && !field.getName().startsWith("s_"))
                columns.add(field);
        }
        fields = getClass().getFields();
        for (Field field : fields) {
            if (!field.getName().startsWith("m_") && !field.getName().startsWith("s_"))
                columns.add(field);
        }
        return new ArrayList<Field>(columns);
    }

    /**
     * Get this class's fields.
     * 
     * @return An array of fields for this class.
     */
    protected List<Field> getColumnFields() {
        Field[] fields = getClass().getDeclaredFields();
        Set<Field> columns = new HashSet<Field>();
        for (Field field : fields) {
            if (!field.getName().startsWith("m_") && !field.getName().startsWith("s_"))
                columns.add(field);
        }
        fields = getClass().getFields();
        for (Field field : fields) {
            if (!field.getName().startsWith("m_") && !field.getName().startsWith("s_"))
                columns.add(field);
        }
        if (!getClass().equals(AREntity.class)) {
            fields = AREntity.class.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getName().startsWith("m_") && !field.getName().startsWith("s_"))
                    columns.add(field);
            }
        }
        return new ArrayList<Field>(columns);
    }

    /**
     * Insert this entity into the database.
     * 
     * @throws ARInflationException
     */
    private void insert() {
        List<Field> columns = _id > 0 ? getColumnFields() : getColumnFieldsWithoutID();
        ContentValues values = new ContentValues(columns.size());
        try {
            for (Field column : columns) {
                Class<?> type = column.getType();
                if (type == byte[].class)
                    values.put(column.getName(), (byte[]) column.get(this));
                else if (type == boolean.class)
                    values.put(column.getName(), column.getBoolean(this) ? 1 : 0);
                else if (AREntity.class.isAssignableFrom(column.getType()))
                    values.put(
                            column.getName(),
                            column.get(this) != null ? String.valueOf(((AREntity) column.get(this))._id)
                                    : null);
                else
                    values.put(column.getName(),
                            column.get(this) != null ? String.valueOf(column.get(this)) : null);
            }
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        _id = m_Database.insert(getTableName(), values);
        m_NeedsInsert = false;
    }

    /**
     * Update this entity in the database.
     * 
     * @throws ARInflationException
     */
    private void update() {
        List<Field> columns = getColumnFieldsWithoutID();
        ContentValues values = new ContentValues(columns.size());
        try {
            for (Field column : columns) {
                if (m_InflationRequired && column.isAnnotationPresent(ARDelayedInflation.class))
                    continue;
                Class<?> type = column.getType();
                if (type == byte[].class)
                    values.put(column.getName(), (byte[]) column.get(this));
                else if (type == boolean.class)
                    values.put(column.getName(), column.getBoolean(this) ? 1 : 0);
                else if (AREntity.class.isAssignableFrom(column.getType()))
                    values.put(
                            column.getName(),
                            column.get(this) != null ? String.valueOf(((AREntity) column.get(this))._id)
                                    : null);
                else
                    values.put(column.getName(),
                            column.get(this) != null ? String.valueOf(column.get(this)) : null);
            }
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        m_Database.update(getTableName(), values, "_id = ?", new String[] { String.valueOf(_id) });
    }

    /**
     * Remove this entity from the database.
     * 
     * @return Whether or the entity was successfully deleted.
     * @throws ARInflationException
     */
    public boolean delete() {
        if (m_Database == null)
            throw new IllegalStateException(
                    "Attempting to delete an entity that does not belong to a database.");
        boolean toRet = m_Database.delete(getTableName(), "_id = ?",
                new String[] { String.valueOf(_id) }) != 0;
        _id = 0;
        m_NeedsInsert = true;
        return toRet;
    }

    /**
     * Save this entity to the database, inserts or updates as needed.
     * 
     * Note: If this entity was loaded from a database, any delayed fields will not be saved until
     * fullyInflate() is called. saveComplete() can be used to bypass this step. Saving the entity
     * to a database other than the one from which it was loaded will cause all fields to be saved.
     * 
     * @param database
     *            The database to store the entity in. This will be set automatically for entities
     *            loaded from the database so the parameterless overload of this method can be used.
     */
    public void save(ARDatabase database) {
        if (m_Database != database) {
            m_Database = database;
            m_NeedsInsert = true;
        }
        save();
    }

    /**
     * Saves this entity to the database, inserts or updates as needed. Saves all fields regardless
     * of whether or not this entity has been fully inflated.
     */
    public void saveComplete() {
        m_InflationRequired = false;
        save();
    }

    /**
     * Saves this entity to the database, inserts or updates as needed.
     * 
     * Note: If this entity was loaded from a database, any delayed fields will not be saved until
     * fullyInflate() is called. saveComplete() can be used to bypass this step.
     */
    public void save() {
        if (m_Database == null)
            throw new IllegalStateException("Database must be set before AREntity can be saved.");
        if (m_NeedsInsert)
            insert();
        else
            update();
        m_Database.getEntityCacheDictionary().set(this);
    }

    /**
     * Inflate this entity using the current row from the given cursor.
     * 
     * @param cursor
     *            The cursor to get object data from.
     * @param database
     *            the database the entity should be initialized with - will be used to load any
     *            referenced entities.
     * @throws ARInflationException
     */
    @SuppressWarnings("unchecked")
    private void inflate(Cursor cursor, ARDatabase database) {
        HashMap<Field, Long> entities = new HashMap<Field, Long>();
        try {
            m_Database = database;
            m_NeedsInsert = false;
            m_InflationRequired = false;
            for (Field field : getColumnFields()) {
                int index = cursor.getColumnIndex(field.getName());
                if (index == -1) {
                    m_InflationRequired = true;
                    continue;
                }
                Class<?> type = field.getType();
                if (type == long.class)
                    field.setLong(this, cursor.getLong(index));
                else if (type == String.class) {
                    String val = cursor.getString(index);
                    field.set(this, val);
                } else if (type == double.class)
                    field.setDouble(this, cursor.getDouble(index));
                else if (type == boolean.class)
                    field.setBoolean(this, cursor.getInt(index) == 1);
                else if (type == byte[].class)
                    field.set(this, cursor.getBlob(index));
                else if (type == int.class)
                    field.setInt(this, cursor.getInt(index));
                else if (type == float.class)
                    field.setFloat(this, cursor.getFloat(index));
                else if (type == short.class)
                    field.setShort(this, cursor.getShort(index));
                else if (AREntity.class.isAssignableFrom(field.getType())) {
                    long id = cursor.getLong(index);
                    if (id > 0)
                        entities.put(field, id);
                    else
                        field.set(this, null);
                } else
                    throw new ARInflationException(String.format(
                            "Class cannot be read from Sqlite3 database. Invalid field: %s.",
                            field.getName()));
            }
            m_Database.getEntityCacheDictionary().set(this);
            for (Field f : entities.keySet()) {
                f.set(this, AREntity.findById((Class<? extends AREntity>) f.getType(),
                        entities.get(f), m_Database));
            }
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
    }

    /**
     * Delete selected entities from the database.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to delete.
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param database
     *            The database used for persistence
     * 
     * @return The number of rows affected.
     * @throws ARInflationException
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> int delete(Class<T> type, String whereClause,
            String[] whereArgs, ARDatabase database) {
        if (database == null)
            throw new IllegalArgumentException("The database to delete from can not be null.");
        try {
            T entity = type.newInstance();
            for (Long id : findIds(type, whereClause, whereArgs, database)) {
                AREntity cachedEntity = database.getEntityCacheDictionary().get(type, id);
                if (cachedEntity != null) {
                    cachedEntity.m_NeedsInsert = true;
                }
            }
            return database.delete(entity.getTableName(), whereClause, whereArgs);
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
    }

    /**
     * Delete all instances of an entity from the database where a column has a specified value.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to delete.
     * @param column
     *            The column to match.
     * @param value
     *            The value required for deletion.
     * @param database
     *            The database used for persistence
     * 
     * @return The number of rows affected.
     * @throws ARInflationException
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> int deleteByColumn(Class<T> type, String column,
            String value, ARDatabase database) {

        for (Long id : findIds(type, String.format("%s = ?", column), new String[] { value },
                database)) {
            AREntity cachedEntity = database.getEntityCacheDictionary().get(type, id);
            if (cachedEntity != null) {
                cachedEntity.m_NeedsInsert = true;
            }
        }
        return delete(type, String.format("%s = ?", column), new String[] { value }, database);
    }

    /**
     * Return all instances of an entity that match the given criteria.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param database
     *            The database used for persistence
     * 
     * @return A generic list of all matching entities.
     * @throws IllegalArgumentException
     * @throws ARInflationException
     */
    public static <T extends AREntity> List<T> find(Class<T> type, String whereClause,
            String[] whereArgs, ARDatabase database) {
        if (database == null)
            throw new IllegalArgumentException("The database to search can not be null.");
        List<T> toRet = new ArrayList<T>();
        try {
            T entity = type.newInstance();
            Cursor c = database.query(entity.getTableName(), entity.getColumns(), whereClause,
                    whereArgs);
            try {
                while (c.moveToNext()) {
                    entity = database.getEntityCacheDictionary().get(type,
                            c.getLong(c.getColumnIndex("_id")));
                    if (entity == null) {
                        entity = type.newInstance();
                        entity.inflate(c, database);
                    }
                    toRet.add(entity);
                }
            } finally {
                c.close();
            }
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        return toRet;
    }

    /**
     * Return all instances of an entity that match the given criteria.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param distinct
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param orderBy
     * @param limit
     *            0 means no limit
     * @param database
     *            The database used for persistence
     * 
     * @return A generic list of all matching entities.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<T> find(Class<T> type, boolean distinct,
            String whereClause, String[] whereArgs, String orderBy, long limit, ARDatabase database) {
        if (database == null)
            throw new IllegalArgumentException("The database to search can not be null.");
        List<T> toRet = new ArrayList<T>();
        try {
            T entity = type.newInstance();
            Cursor c = database.query(distinct, entity.getTableName(), entity.getColumns(),
                    whereClause, whereArgs, null, null, orderBy, limit > 0 ? String.valueOf(limit)
                            : null);
            try {
                while (c.moveToNext()) {
                    entity = database.getEntityCacheDictionary().get(type,
                            c.getLong(c.getColumnIndex("_id")));
                    if (entity == null) {
                        entity = type.newInstance();
                        entity.inflate(c, database);
                    }
                    toRet.add(entity);
                }
            } finally {
                c.close();
            }
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        return toRet;
    }

    /**
     * Return all instances of an entity from the database where a column has a specified value.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param column
     *            The column to match.
     * @param value
     *            The desired value.
     * @param database
     *            The database used for persistence
     * 
     * @return A generic list of all matching entities.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<T> findByColumn(Class<T> type, String column,
            Object value, ARDatabase database) {
        return find(type, String.format("%s = ?", column), new String[] { String.valueOf(value) },
                database);
    }

    /**
     * Return all instances of an entity from the database where a column has a specified value.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param column
     *            The column to match.
     * @param value
     *            The desired value.
     * @param orderBy
     *            The column to order by.
     * @param database
     *            The database used for persistence
     * 
     * @return A generic list of all matching entities.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<T> findByColumn(Class<T> type, String column,
            Object value, String orderBy, ARDatabase database) {
        return find(type, false, String.format("%s = ?", column),
                new String[] { String.valueOf(value) }, orderBy, 0, database);
    }

    /**
     * Return the instance of an entity with a matching id.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entity to return.
     * @param id
     *            The desired ID.
     * @param database
     *            The database used for persistence
     * @throws IllegalArgumentException
     * @return The matching entity.
     */
    public static <T extends AREntity> T findById(Class<T> type, long id, ARDatabase database) {
        return findById(type, id, database, false);
    }

    /**
     * Return the instance of an entity with a matching id.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entity to return.
     * @param id
     *            The desired ID.
     * @param database
     *            The database used for persistence.
     * @param fullyInflate
     *            Whether or not to grab ARDelayedInflation fields.
     * @throws IllegalArgumentException
     * @return The matching entity.
     */
    public static <T extends AREntity> T findById(Class<T> type, long id, ARDatabase database,
            boolean fullyInflate) {
        if (database == null)
            throw new IllegalArgumentException("The database to search can not be null.");
        T entity = database.getEntityCacheDictionary().get(type, id);
        if (entity != null) {
            if (entity.m_InflationRequired && fullyInflate)
                entity.fullyInflate();
            return entity;
        }
        try {
            entity = type.newInstance();
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        Cursor c = database.query(entity.getTableName(), entity.getColumns(fullyInflate),
                "_id = ?", new String[] { String.valueOf(id) });
        try {
            if (!c.moveToNext())
                return null;
            entity.inflate(c, database);
        } finally {
            c.close();
        }
        return entity;
    }

    /**
     * Return all instances of an entity from the database.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param database
     *            The database used for persistence
     * @return A generic list of all matching entities.
     * @throws IllegalArgumentException
     * @throws ARInflationException
     */
    public static <T extends AREntity> List<T> findAll(Class<T> type, ARDatabase database) {
        return find(type, null, null, database);
    }

    /**
     * Return all ids for instances of an entity that match the given criteria.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param database
     *            The database used for persistence
     * 
     * @return A list of all matching entity ids.
     * @throws IllegalArgumentException
     * @throws ARInflationException
     */
    public static <T extends AREntity> List<Long> findIds(Class<T> type, String whereClause,
            String[] whereArgs, ARDatabase database) {
        if (database == null)
            throw new IllegalArgumentException("The database to search can not be null.");
        List<Long> toRet = new ArrayList<Long>();
        try {
            T entity = type.newInstance();
            Cursor c = database.query(entity.getTableName(), new String[] { "_id" }, whereClause,
                    whereArgs);
            try {
                while (c.moveToNext()) {
                    toRet.add(c.getLong(0));
                }
            } finally {
                c.close();
            }
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        return toRet;
    }

    /**
     * Return all ids for instances of an entity that match the given criteria.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param distinct
     * @param whereClause
     *            The condition to match (Don't include "where").
     * @param whereArgs
     *            The arguments to replace "?" with.
     * @param orderBy
     * @param limit
     *            0 means no limit
     * @param database
     *            The database used for persistence
     * 
     * @return A list of all matching entity ids.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<Long> findIds(Class<T> type, boolean distinct,
            String whereClause, String[] whereArgs, String orderBy, long limit, ARDatabase database) {
        if (database == null)
            throw new IllegalArgumentException("The database to search can not be null.");
        List<Long> toRet = new ArrayList<Long>();
        try {
            T entity = type.newInstance();
            Cursor c = database.query(distinct, entity.getTableName(), new String[] { "_id" },
                    whereClause, whereArgs, null, null, orderBy, limit > 0 ? String.valueOf(limit)
                            : null);
            try {
                while (c.moveToNext()) {
                    toRet.add(c.getLong(0));
                }
            } finally {
                c.close();
            }
        } catch (InstantiationException ex) {
            throw new ARInflationException(ex);
        } catch (IllegalAccessException ex) {
            throw new ARInflationException(ex);
        }
        return toRet;
    }

    /**
     * Return all ids for instances of an entity from the database where a column has a specified
     * value.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param column
     *            The column to match.
     * @param value
     *            The desired value.
     * @param database
     *            The database used for persistence
     * 
     * @return A list of all matching entity ids.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<Long> findIdsByColumn(Class<T> type, String column,
            Object value, ARDatabase database) {
        return findIds(type, String.format("%s = ?", column),
                new String[] { String.valueOf(value) }, database);
    }

    /**
     * Return all ids for instances of an entity from the database where a column has a specified
     * value.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param column
     *            The column to match.
     * @param value
     *            The desired value.
     * @param orderBy
     *            The column to order by.
     * @param database
     *            The database used for persistence
     * 
     * @return A list of all matching entity ids.
     * @throws IllegalArgumentException
     */
    public static <T extends AREntity> List<Long> findIdsByColumn(Class<T> type, String column,
            Object value, String orderBy, ARDatabase database) {
        return findIds(type, false, String.format("%s = ?", column),
                new String[] { String.valueOf(value) }, orderBy, 0, database);
    }

    /**
     * Return all ids for instances of an entity from the database.
     * 
     * @param <T>
     *            Any AREntity class.
     * @param type
     *            The class of the entities to return.
     * @param database
     *            The database used for persistence
     * @return A list of all matching entity ids.
     * @throws IllegalArgumentException
     * @throws ARInflationException
     */
    public static <T extends AREntity> List<Long> findAllIds(Class<T> type, ARDatabase database) {
        return findIds(type, null, null, database);
    }

    /**
     * Return the number of entities in the database of the input type.
     * 
     * @param <T>
     * @param type
     *            The type of entity to search for.
     * @param database
     *            The database to search.
     * @return
     */
    public static <T extends AREntity> long count(Class<T> type, ARDatabase database) {
        return count(type, null, null, database);
    }

    /**
     * Return the number of matching entities in the database of the input type.
     * 
     * @param <T>
     * @param type
     *            The type of entity to search for.
     * @param column
     *            The column to match.
     * @param value
     *            The desired value.
     * @param database
     *            The database to search.
     * @return
     */
    public static <T extends AREntity> long countByColumn(Class<T> type, String column,
            Object value, ARDatabase database) {
        return count(type, false, String.format("%s = ?"), new String[] { String.valueOf(value) },
                0, database);
    }

    /**
     * Return the number of matching entities in the database of the input type.
     * 
     * @param <T>
     * @param type
     *            The type of entity to search for.
     * @param whereClause
     *            A normal SQL where clause without the "WHERE" keyword. Use ? as a place holder for
     *            parameters.
     * @param whereArgs
     *            If any place holders are present in whereClause their values must be present, in
     *            order, in the whereArgs array.
     * @param database
     *            The database to search.
     * @return
     */
    public static <T extends AREntity> long count(Class<T> type, String whereClause,
            String[] whereArgs, ARDatabase database) {
        return count(type, false, whereClause, whereArgs, 0, database);
    }

    /**
     * Return the number of matching entities in the database of the input type.
     * 
     * @param <T>
     * @param type
     *            The type of entity to search for.
     * @param distinct
     *            Whether or not to skip multiples of a given row if present in the database.
     * @param whereClause
     *            A normal SQL where clause without the "WHERE" keyword. Use ? as a place holder for
     *            parameters.
     * @param whereArgs
     *            If any place holders are present in whereClause their values must be present, in
     *            order, in the whereArgs array.
     * @param limit
     *            The maximum count to return.
     * @param database
     *            The database to search.
     * @return
     */
    public static <T extends AREntity> long count(Class<T> type, boolean distinct,
            String whereClause, String[] whereArgs, long limit, ARDatabase database) {
        StringBuilder sb = new StringBuilder("SELECT ");
        if (distinct) {
            sb.append("DISTINCT ");
        }
        sb.append("COUNT(*) FROM ").append(type.getName());
        if (whereClause != null) {
            sb.append(" WHERE ").append(whereClause);
        }
        if (limit > 0) {
            sb.append(" LIMIT ").append(limit);
        }
        Cursor c = database.rawQuery(sb.toString(), whereArgs);
        if (c.moveToNext()) {
            return c.getLong(0);
        }
        return 0;
    }

    /**
     * Fully inflates the entity, including delayed fields. This will completely reload the entity,
     * so make sure you've saved any changes you want to keep or they will be rolled back to the
     * value in the database.
     */
    public void fullyInflate() {
        if (m_Database == null)
            throw new IllegalStateException(
                    "The entity must belong to a database before it can be fully inflated.");
        Cursor c = m_Database.query(this.getTableName(), getColumns(true), "_id = ?",
                new String[] { String.valueOf(_id) });
        try {
            if (c.moveToNext())
                inflate(c, m_Database);
        } finally {
            c.close();
        }
    }

    /**
     * A test to see if the entity is fully inflated.
     * 
     * @return true if ARDelayedInflation fields have not been filled.
     */
    public boolean isFullyInflated() {
        return !m_InflationRequired;
    }
}
