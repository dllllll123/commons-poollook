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
package org.apache.commons.pool3.proxy;

import org.apache.commons.pool3.UsageTracking;

abstract class BaseProxiedPool<T> {

    private final ProxySource<T> proxySource;

    BaseProxiedPool(final ProxySource<T> proxySource) {
        this.proxySource = proxySource;
    }

    protected final T createProxy(final T pooledObject, final Object sourcePool) {
        return proxySource.createProxy(pooledObject, getUsageTracking(sourcePool));
    }

    protected final ProxySource<T> getProxySource() {
        return proxySource;
    }

    protected final T resolveProxy(final T proxy) {
        return proxySource.resolveProxy(proxy);
    }

    @SuppressWarnings("unchecked")
    private UsageTracking<T> getUsageTracking(final Object sourcePool) {
        if (sourcePool instanceof UsageTracking<?>) {
            return (UsageTracking<T>) sourcePool;
        }
        return null;
    }
}
