import org.apache.commons.pool3.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.impl.DefaultPooledObject;

public class KeyedStringBufferFactory
    extends BaseKeyedPooledObjectFactory<String, StringBuffer, RuntimeException> {

    @Override
    public StringBuffer create(final String key) {
        return new StringBuffer(Math.max(16, key.length()));
    }

    @Override
    public PooledObject<StringBuffer> wrap(final StringBuffer value) {
        return new DefaultPooledObject<>(value);
    }

    @Override
    public void passivateObject(final String key, final PooledObject<StringBuffer> pooledObject) {
        pooledObject.getObject().setLength(0);
    }
}
