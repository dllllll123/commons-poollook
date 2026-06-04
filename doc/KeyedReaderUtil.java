import java.io.IOException;
import java.io.Reader;

import org.apache.commons.pool3.KeyedObjectPool;

public class KeyedReaderUtil {

    private final KeyedObjectPool<String, StringBuffer, RuntimeException> pool;

    public KeyedReaderUtil(final KeyedObjectPool<String, StringBuffer, RuntimeException> pool) {
        this.pool = pool;
    }

    public String readToString(final String key, final Reader in) throws IOException {
        StringBuffer buffer = null;
        try {
            buffer = pool.borrowObject(key);
            for (int c = in.read(); c != -1; c = in.read()) {
                buffer.append((char) c);
            }
            return buffer.toString();
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to borrow buffer from pool for key " + key, e);
        } finally {
            try {
                in.close();
            } catch (final Exception e) {
            }
            try {
                if (buffer != null) {
                    pool.returnObject(key, buffer);
                }
            } catch (final Exception e) {
            }
        }
    }
}
