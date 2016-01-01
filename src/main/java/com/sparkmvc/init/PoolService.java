package com.sparkmvc.init;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Created by Nurmuhammad on 28-Nov-15.
 */
public class PoolService {

    private static final Logger logger = LoggerFactory.getLogger(PoolService.class);

    public static PoolService instance;
    ComboPooledDataSource cpds;

    private PoolService() {
        init();
    }

    public static PoolService getInstance() {
        if(instance==null){
            instance = new PoolService();
        }
        return instance;
    }

    private void init() {
        try {
            getComboPooledDataSource();
        } catch (Exception e) {
            logger.error("Error when initializing PoolService.", e);
        }
    }

    private void destroy() {
        if(cpds!=null){
            cpds.close();
            cpds = null;
        }
        System.gc();
    }

    private ComboPooledDataSource getComboPooledDataSource() throws PropertyVetoException {

        if(cpds!=null) return cpds;

        cpds = new ComboPooledDataSource();
        cpds.setDriverClass(Config.get("database.driver"));
        cpds.setJdbcUrl(Config.get("database.jdbc.url"));
        cpds.setUser(Config.get("database.user"));
        cpds.setPassword(Config.get("database.password"));

        cpds.setMaxPoolSize(Config.get("ds.pool.max-size", 30));
        cpds.setMinPoolSize(Config.get("ds.pool.min-size", 3));
        cpds.setMaxIdleTime(Config.get("ds.pool.max-idle-time", 60));
        cpds.setMaxStatements(Config.get("ds.pool.max-statements", 600));
        cpds.setMaxStatementsPerConnection(Config.get("ds.pool.max-statements-per-connection", 60));

        return cpds;
    }

    public static ComboPooledDataSource getDataSource(){
        try {
            return getInstance().getComboPooledDataSource();
        } catch (PropertyVetoException e) {
            logger.error("Error when getting datasource from the pool.", e);
            throw new RuntimeException("Error when getting datasource from the pool.");
        }
    }

    public static Connection getConnection() {
        try {
            return getInstance().cpds.getConnection();
        } catch (SQLException e) {
            logger.error("Error when get connection from the pool.", e);
            throw new RuntimeException("Error when get connection from the pool.");
        }
    }

}