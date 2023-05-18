/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.tpc.apps;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.noop.Noop;
import com.hazelcast.spi.properties.ClusterProperty;

/**
 * There is great variability between the runs. I believe this is related to the amount of batching that happens at the
 * network level when concurrency level is set higher than 1.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class RemoteNoopBenchmark {
    public static final long operations = 1000000;
    public static final int concurrency = 1;

    public static void main(String[] args) throws Exception {
        System.setProperty(ClusterProperty.TPC_ENABLED.getName(), "true");
        System.setProperty(ClusterProperty.TPC_EVENTLOOP_COUNT.getName(), "1");

        HazelcastInstance localNode = Hazelcast.newHazelcastInstance();
        HazelcastInstance remoteNode = Hazelcast.newHazelcastInstance();

        System.out.println("Waiting for partition tables to settle");
        Thread.sleep(5000);
        System.out.println("Waiting for partition tables to settle: done");
        int partitionId = remoteNode.getPartitionService().getPartitions().iterator().next().getPartitionId();

        Noop table = localNode.getProxy(Noop.class, "sometable");

        long iterations = operations / concurrency;

        long startMs = System.currentTimeMillis();
        long count = 0;
        for (int k = 0; k < iterations; k++) {

            if (count % 100_000 == 0) {
                System.out.println("at k:" + count);
            }

            table.concurrentNoop(concurrency, partitionId);
            count += concurrency;
        }

        System.out.println("Done");

        long duration = System.currentTimeMillis() - startMs;
        System.out.println("Throughput: " + (operations * 1000.0f / duration) + " op/s");
        localNode.shutdown();
        remoteNode.shutdown();
    }
}
