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

package io.shardingsphere.orchestration.reg.zookeeper.natived.client.retry;

import io.shardingsphere.orchestration.reg.zookeeper.natived.client.action.IZookeeperProvider;
import io.shardingsphere.orchestration.reg.zookeeper.natived.client.zookeeper.provider.BaseZookeeperProvider;
import org.apache.zookeeper.KeeperException;

public abstract class TestResultCallable<T> extends RetryResultCallable<T> {
    
    private int count;
    
    public TestResultCallable(final IZookeeperProvider provider, final DelayRetryPolicy delayRetryPolicy) {
        super(provider, delayRetryPolicy);
    }
    
    @Override
    public final void call() throws KeeperException, InterruptedException {
        if (count < 2) {
            count++;

            // need dependent zk beat version
            //((BaseZookeeperProvider)getProvider()).getHolder().getZooKeeper().getTestable().injectSessionExpiration();
            ((BaseZookeeperProvider) getProvider()).getHolder().close();
        }
        test();
    }
    
    /**
     * Test exec.
     *
     * @throws KeeperException Zookeeper Exception
     * @throws InterruptedException InterruptedException
     */
    public abstract void test() throws KeeperException, InterruptedException;
}
