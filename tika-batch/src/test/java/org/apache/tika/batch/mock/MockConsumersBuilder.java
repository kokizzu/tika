/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.batch.mock;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.w3c.dom.Node;

import org.apache.tika.batch.ConsumersManager;
import org.apache.tika.batch.FileResource;
import org.apache.tika.batch.fs.builders.BasicTikaFSConsumersBuilder;

public class MockConsumersBuilder extends BasicTikaFSConsumersBuilder {

    @Override
    public ConsumersManager build(Node node, Map<String, String> runtimeAttributes, ArrayBlockingQueue<FileResource> queue) {
        ConsumersManager manager = super.build(node, runtimeAttributes, queue);

        boolean hangOnInit = runtimeAttributes.containsKey("hangOnInit");
        boolean hangOnShutdown = runtimeAttributes.containsKey("hangOnShutdown");
        return new MockConsumersManager(manager, hangOnInit, hangOnShutdown);
    }
}
