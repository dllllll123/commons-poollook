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

import org.apache.commons.pool3.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool3.KeyedObjectPool;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericKeyedObjectPool;

/**
 * Example {@link BaseKeyedPooledObjectFactory} that creates
 * {@link StringBuilder} instances keyed by the expected
 * buffer size category.
 * <p>
 * {@code GenericKeyedObjectPool} maintains a separate pool of
 * objects per key. This is useful when you want to reuse
 * objects that belong to different categories, such as:
 * </p>
 * <ul>
 *   <li>network connections keyed by remote host</li>
 *   <li>formatters keyed by locale or format pattern</li>
 *   <li>buffers keyed by expected size</li>
 * </ul>
 */
public class KeyedPoolExample {

    private KeyedObjectPool<String, StringBuilder> pool;

    public KeyedPoolExample(KeyedObjectPool<String, StringBuilder> pool) {
        this.pool = pool;
    }

    /**
     * Builds a string by repeatedly appending lines keyed by a
     * category that determines the initial buffer capacity.
     * <p>
     * Each call borrows a {@link StringBuilder} from the pool
     * under the given key and returns it when done, ensuring
     * that buffers sized for different workloads do not
     * interfere with one another.
     * </p>
     *
     * @param key   category key (determines initial capacity)
     * @param lines lines to append
     * @return the concatenated result
     */
    public String build(String key, String... lines) {
        StringBuilder buf = null;
        try {
            buf = pool.borrowObject(key);
            for (String line : lines) {
                buf.append(line);
            }
            return buf.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to borrow buffer from pool for key=" + key, e);
        } finally {
            try {
                if (buf != null) {
                    pool.returnObject(key, buf);
                }
            } catch (Exception e) {
                // ignored
            }
        }
    }

    /**
     * Example {@code KeyedPooledObjectFactory} that creates
     * {@link StringBuilder} instances with different initial
     * capacities based on the key.
     */
    public static class Factory
            extends BaseKeyedPooledObjectFactory<String, StringBuilder, RuntimeException> {

        @Override
        public StringBuilder create(String key) {
            int capacity;
            switch (key) {
                case "SMALL":
                    capacity = 64;
                    break;
                case "MEDIUM":
                    capacity = 256;
                    break;
                case "LARGE":
                    capacity = 1024;
                    break;
                default:
                    capacity = 128;
            }
            return new StringBuilder(capacity);
        }

        @Override
        public PooledObject<StringBuilder> wrap(StringBuilder value) {
            return new DefaultPooledObject<>(value);
        }

        /**
         * When an object is returned to the pool, clear the buffer.
         */
        @Override
        public void passivateObject(String key, PooledObject<StringBuilder> pooledObject) {
            pooledObject.getObject().setLength(0);
        }
    }

    public static void main(String[] args) {
        KeyedPoolExample example = new KeyedPoolExample(
                new GenericKeyedObjectPool<>(new Factory()));

        String result = example.build("MEDIUM",
                "Hello, ", "Keyed", "ObjectPool", "!");

        System.out.println(result);
    }
}