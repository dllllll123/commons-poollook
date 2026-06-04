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
package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.apache.commons.lang3.ThreadUtils;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;
import org.junit.jupiter.api.Test;

class TestResilientPooledObjectFactory {

    /**
     * Factory that suffers outages and fails in configurable ways when it is down.
     */
    class FailingFactory implements PooledObjectFactory<String, Exception> {

        /** Whether or not the factory is up */
        private boolean up = true;

        /** Whether or not to fail silently */
        private boolean silentFail = true;

        /** Whether or not to hang */
        private boolean hang;

        @Override
        public void activateObject(final PooledObject<String> p) throws Exception {
        }

        public void crash() {
            this.up = false;
        }

        @Override
        public void destroyObject(final PooledObject<String> p) throws Exception {
        }

        @Override
        public PooledObject<String> makeObject() throws Exception {
            if (up) {
                return new DefaultPooledObject<>(UUID.randomUUID().toString());
            }
            if (!silentFail) {
                throw new Exception("makeObject failed");
            }
            if (hang) {
                while (!up) {
                    ThreadUtils.sleepQuietly(Duration.ofSeconds(1));
                }
            }
            return null;
        }

        @Override
        public void passivateObject(final PooledObject<String> p) throws Exception {
        }

        public void recover() {
            this.up = true;
        }

        public void setHang(final boolean hang) {
            this.hang = hang;
        }

        public void setSilentFail(final boolean silentFail) {
            this.silentFail = silentFail;
        }

        @Override
        public boolean validateObject(final PooledObject<String> p) {
            return up;
        }
    }

    @Test
    void testAdderStartStop() throws Exception {
        final FailingFactory ff = new FailingFactory();
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(200), Duration.ofMinutes(10), Duration.ofMillis(20));
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        rf.setPool(pool);
        rf.startMonitor();
        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                } catch (final Exception e) {
                }
            }
        }.start();
        ThreadUtils.sleepQuietly(Duration.ofMillis(50));
        ff.crash();
        try {
            pool.returnObject(s1);
        } catch (final Exception e) {
        }
        ThreadUtils.sleepQuietly(Duration.ofMillis(100));
        assertTrue(rf.isAdderRunning());
        ff.recover();
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertEquals(0, pool.getNumWaiters());
        assertTrue(rf.isAdderRunning());
        pool.setMaxTotal(10);
        for (int i = 0; i < 6; i++) {
            pool.addObject();
        }
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertTrue(rf.isUp());
        assertFalse(rf.isAdderRunning());
    }

    @Test
    void testConstructorWithDefaults() {
        final FailingFactory ff = new FailingFactory();
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff);
        assertFalse(rf.isMonitorRunning());
        assertFalse(rf.isAdderRunning());
        assertEquals(ResilientPooledObjectFactory.getDefaultLogSize(), rf.getLogSize());
        assertEquals(ResilientPooledObjectFactory.getDefaultTimeBetweenChecks(), rf.getTimeBetweenChecks());
        assertEquals(ResilientPooledObjectFactory.getDefaultDelay(), rf.getDelay());
        assertEquals(ResilientPooledObjectFactory.getDefaultLookBack(), rf.getLookBack());
        assertEquals(0, rf.getMakeObjectLog().size());
        rf.setLogSize(5);
        assertEquals(5, rf.getLogSize());
        rf.setTimeBetweenChecks(Duration.ofMillis(200));
    }

    @Test
    void testExceptionCountsAccumulateForSameExceptionType() {
        final PooledObjectFactory<String, Exception> factory = new PooledObjectFactory<String, Exception>() {
            private int failuresRemaining = 2;

            @Override
            public void activateObject(final PooledObject<String> p) throws Exception {
            }

            @Override
            public void destroyObject(final PooledObject<String> p) throws Exception {
            }

            @Override
            public PooledObject<String> makeObject() throws Exception {
                if (failuresRemaining-- > 0) {
                    throw new Exception("makeObject failed");
                }
                return new DefaultPooledObject<>(UUID.randomUUID().toString());
            }

            @Override
            public void passivateObject(final PooledObject<String> p) throws Exception {
            }

            @Override
            public boolean validateObject(final PooledObject<String> p) {
                return true;
            }
        };
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(factory);

        assertThrows(Exception.class, rf::makeObject);
        assertThrows(Exception.class, rf::makeObject);
        assertEquals(2, rf.getExceptionCount(Exception.class));
    }

    @Test
    void testIsMonitorRunning() throws Exception {
        final FailingFactory ff = new FailingFactory();
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(200), Duration.ofMinutes(10), Duration.ofMillis(20));
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        rf.setPool(pool);
        rf.startMonitor();
        assertTrue(rf.isMonitorRunning());
        rf.stopMonitor();
        assertFalse(rf.isMonitorRunning());
        rf.startMonitor();
        pool.close();
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertFalse(rf.isMonitorRunning());
    }

    @Test
    void testNulls() throws Exception {
        final FailingFactory ff = new FailingFactory();
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(50), Duration.ofMinutes(10), Duration.ofMillis(50));
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        rf.setPool(pool);
        rf.startMonitor(Duration.ofMillis(20));

        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        ff.crash();
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                    pool.returnObject(s);
                } catch (final Exception e) {
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                    pool.returnObject(s);
                } catch (final Exception e) {
                }
            }
        }.start();
        ThreadUtils.sleepQuietly(Duration.ofMillis(50));
        pool.returnObject(s1);
        pool.returnObject(s2);
        assertEquals(0, pool.getNumIdle());
        assertTrue(pool.getNumWaiters() > 0);
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertFalse(rf.isUp());
        ff.recover();
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertEquals(0, pool.getNumWaiters());
        pool.close();
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertFalse(rf.isAdderRunning());
        assertFalse(rf.isMonitorRunning());
    }

    @Test
    void testTransientFailure() throws Exception {
        final FailingFactory ff = new FailingFactory();
        ff.setHang(false);
        ff.setSilentFail(false);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(100), Duration.ofMinutes(10), Duration.ofMillis(100));
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        rf.setPool(pool);
        rf.startMonitor(Duration.ofMillis(20));
        assertTrue(rf.isUp());
        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        new Thread() {
            @Override
            public void run() {
                try {
                    pool.borrowObject();
                } catch (final Exception e) {
                }
            }
        }.start();
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        ff.crash();
        assertTrue(rf.isUp());
        assertEquals(1, pool.getNumWaiters());
        pool.returnObject(s1);
        pool.returnObject(s2);
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumWaiters());
        ThreadUtils.sleepQuietly(Duration.ofMillis(100));
        assertFalse(rf.isUp());
        assertTrue(rf.isAdderRunning());
        assertEquals(1, pool.getNumWaiters());
        ff.recover();
        ThreadUtils.sleepQuietly(Duration.ofMillis(100));
        assertTrue(pool.getNumWaiters() == 0);
        pool.setMaxTotal(10);
        for (int i = 0; i < 5; i++) {
            pool.addObject();
        }
        ThreadUtils.sleepQuietly(Duration.ofMillis(200));
        assertTrue(rf.isUp());
        assertFalse(rf.isAdderRunning());

        pool.close();
        rf.stopMonitor();
    }
}
