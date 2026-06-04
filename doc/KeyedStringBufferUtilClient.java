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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.pool3.impl.GenericKeyedObjectPool;

/**
 * Instantiates and uses a KeyedStringBufferUtil. The GenericKeyedObjectPool
 * supplied to the constructor will have default configuration properties.
 * This demonstrates how keyed pool can manage different categories of
 * objects (in this case, StringBuffers with different initial capacities).
 */
public class KeyedStringBufferUtilClient {

    public static void main(String[] args) {
        KeyedStringBufferUtil keyedUtil = new KeyedStringBufferUtil(
            new GenericKeyedObjectPool<Integer, StringBuffer, RuntimeException>(new KeyedStringBufferFactory())
        );
        
        // Use small capacity for short string
        Reader shortReader = new StringReader("Hello World!");
        try {
            System.out.println("Short string: " + keyedUtil.readToString(shortReader, 16));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Use large capacity for longer string
        Reader longReader = new StringReader("This is a much longer string that benefits from a larger initial capacity.");
        try {
            System.out.println("Long string: " + keyedUtil.readToString(longReader, 128));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
