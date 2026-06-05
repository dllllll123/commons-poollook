package org.apache.commons.pool3.proxy;

import org.apache.commons.pool3.UsageTracking;

/**
 * Base class for proxied object pools.
 *
 * @param <V> type of the pooled object
 * @since 3.0
 */
abstract class AbstractProxiedObjectPool<V> {

    private final ProxySource<V> proxySource;

    /**
     * Constructs a new proxied object pool.
     *
     * @param proxySource The source of the proxy objects
     */
    AbstractProxiedObjectPool(final ProxySource<V> proxySource) {
        this.proxySource = proxySource;
    }

    /**
     * Creates a new proxy object, wrapping the given pooled object.
     *
     * @param pooledObject  The object to wrap
     * @param pool The object pool to wrap
     * @return the new proxy object
     */
    @SuppressWarnings("unchecked")
    V createProxy(final V pooledObject, final Object pool) {
        UsageTracking<V> usageTracking = null;
        if (pool instanceof UsageTracking) {
            usageTracking = (UsageTracking<V>) pool;
        }
        return proxySource.createProxy(pooledObject, usageTracking);
    }

    /**
     * Resolves the wrapped object from the given proxy.
     *
     * @param proxy The proxy object
     * @return The pooled object wrapped by the given proxy
     */
    V resolveProxy(final V proxy) {
        return proxySource.resolveProxy(proxy);
    }

    /**
     * Gets the proxy source.
     *
     * @return the proxy source
     */
    ProxySource<V> getProxySource() {
        return proxySource;
    }
}
