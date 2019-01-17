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

package io.shardingsphere.transaction.saga.servicecomb.transport;

import io.shardingsphere.transaction.saga.SagaTransaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public final class ShardingTransportFactoryTest {
    
    @Mock
    private SagaTransaction sagaTransaction;
    
    @Test
    public void assertCacheTransport() {
        ShardingTransportFactory.getInstance().cacheTransport(sagaTransaction);
        assertThat(ShardingTransportFactory.getInstance().getTransport(), instanceOf(ShardingSQLTransport.class));
    }
    
    @Test
    public void assertRemove() {
        ShardingTransportFactory.getInstance().cacheTransport(sagaTransaction);
        ShardingTransportFactory.getInstance().remove();
        assertNull(ShardingTransportFactory.getInstance().getTransport());
    }
}
