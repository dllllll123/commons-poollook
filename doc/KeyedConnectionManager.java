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
import java.sql.SQLException;

import org.apache.commons.pool3.KeyedObjectPool;

/**
 * Manages database connections using a KeyedObjectPool, where each key
 * identifies a distinct database (e.g., "primary", "replica").
 */
public class KeyedConnectionManager {

    private KeyedObjectPool<String, Connection> pool;

    public KeyedConnectionManager(KeyedObjectPool<String, Connection> pool) {
        this.pool = pool;
    }

    /**
     * Executes a query against the database identified by the given key,
     * returning the connection to the pool when done.
     */
    public void executeQuery(String dbKey, String sql)
        throws SQLException {
        Connection conn = null;
        try {
            conn = pool.borrowObject(dbKey);
            conn.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to borrow connection from pool: " + e.toString());
        } finally {
            try {
                if (null != conn) {
                    pool.returnObject(dbKey, conn);
                }
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
