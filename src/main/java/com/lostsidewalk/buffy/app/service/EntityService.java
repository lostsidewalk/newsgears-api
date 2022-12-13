package com.lostsidewalk.buffy.app.service;

import com.lostsidewalk.buffy.AbstractDao;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class EntityService<T> {

    protected abstract AbstractDao<T> getDao();

    public final List<T> findAll() throws DataAccessException {
        return getDao().findAll();
    }

    public final T findById(Long id) throws DataAccessException {
        return getDao().findById(id);
    }

    public final T findByName(String name) throws DataAccessException {
        return getDao().findByName(name);
    }

    public final T add(T entity) throws DataAccessException, DataUpdateException {
        if (entity != null) {
            return getDao().add(entity);
        }

        return null;
    }

    public final T update(T entity) throws DataAccessException {
        return getDao().update(entity);
    }

    public final void delete(Long id) throws DataAccessException, DataUpdateException {
        getDao().delete(id);
    }
}
