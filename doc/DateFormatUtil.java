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

import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.pool3.KeyedObjectPool;

/**
 * Maintains a keyed pool of DateFormats to format dates.
 */
public class DateFormatUtil {

    private KeyedObjectPool<String, DateFormat> pool;

    public DateFormatUtil(KeyedObjectPool<String, DateFormat> pool) {
        this.pool = pool;
    }

    /**
     * Formats a Date to a String using the specified format pattern.
     */
    public String format(Date date, String formatPattern) {
        DateFormat format = null;
        try {
            format = pool.borrowObject(formatPattern);
            return format.format(date);
        } catch (Exception e) {
            throw new RuntimeException("Unable to borrow DateFormat from pool" + e.toString());
        } finally {
            try {
                if (null != format) {
                    pool.returnObject(formatPattern, format);
                }
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
