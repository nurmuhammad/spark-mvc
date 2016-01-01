package com.sparkmvc.init;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.sql2o.Sql2o;

/**
 * Created by Nurmuhammad on 29-Nov-15.
 */
public class Sql2oFactory extends BasePooledObjectFactory<Sql2o> {

    @Override
    public Sql2o create() throws Exception {
        return new Sql2o(PoolService.getDataSource());
    }

    @Override
    public PooledObject<Sql2o> wrap(Sql2o obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void passivateObject(PooledObject<Sql2o> pooledObject) {

    }

    @Override
    public void destroyObject(PooledObject<Sql2o> pooledObject) throws Exception  {

    }

}
