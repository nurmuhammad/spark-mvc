package com.sparkmvc.dao;

import com.sparkmvc.init.PoolService;
import com.sparkmvc.init.Sql2oFactory;
import com.sparkmvc.model.aModel;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

/**
 * @author nurmuhammad
 */

public abstract class GenericDao<E> {

    private static final Logger logger = LoggerFactory.getLogger(GenericDao.class);

    public static final ObjectPool<Sql2o> pool = new GenericObjectPool<>(new Sql2oFactory());

    public Class<E> className;

    @SuppressWarnings("unchecked")
    public GenericDao() {
        try {
            className = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        } catch (Throwable e) {
            logger.error("Throws error when detecting generic class.", e);
            throw new RuntimeException("Please see the logs.");
        }
    }

    public Sql2o sql2o() {
        try {
            return pool.borrowObject();

        } catch (Exception e) {
            logger.error("Throws error when borrowing Sql2o object from the pool.", e);
        }
        logger.info("Trying return new Sql2o object outside from the pool.");
        return new Sql2o(PoolService.getDataSource());
    }

    public void returnSql2o(Sql2o sql2o) {
        try {
            sql2o.getDefaultColumnMappings().clear();
            pool.returnObject(sql2o);
        } catch (Exception e) {
            logger.error("Throws error when returning Sql2o object to the pool.", e);
        }
    }

    public Query addColumnMapping(Query query) {
        Map<String, String> map = aModel.getColumnMaps().get(className);
        if (map == null || map.isEmpty()) return query;
        map.keySet().stream().forEach(s -> {
            query.addColumnMapping(s, map.get(s));
        });

        return query;
    }

}
