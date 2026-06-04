/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is one of the source files for the examples contained in
 * /src/site/xdoc/examples.xml
 * It is not intended to be included in a source release.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.pool3.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.impl.DefaultPooledObject;

/**
 * Example KeyedPooledObjectFactory that creates database Connections
 * keyed by a database identifier string (e.g., "primary", "replica").
 */
public class ConnectionKeyedFactory
    extends BaseKeyedPooledObjectFactory<String, Connection, SQLException> {

    @Override
    public Connection create(String key) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/" + key;
        return DriverManager.getConnection(url);
    }

    @Override
    public PooledObject<Connection> wrap(Connection connection) {
        return new DefaultPooledObject<Connection>(connection);
    }

    /**
     * When a connection is returned to the pool, ensure it is still valid
     * and reset auto-commit to the default state.
     */
    @Override
    public void passivateObject(String key, PooledObject<Connection> pooledObject)
        throws SQLException {
        Connection conn = pooledObject.getObject();
        if (conn.isClosed()) {
            throw new SQLException("Connection is closed");
        }
        conn.setAutoCommit(true);
    }

    @Override
    public boolean validateObject(String key, PooledObject<Connection> pooledObject) {
        try {
            return !pooledObject.getObject().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void destroyObject(String key, PooledObject<Connection> pooledObject)
        throws SQLException {
        pooledObject.getObject().close();
    }
}
