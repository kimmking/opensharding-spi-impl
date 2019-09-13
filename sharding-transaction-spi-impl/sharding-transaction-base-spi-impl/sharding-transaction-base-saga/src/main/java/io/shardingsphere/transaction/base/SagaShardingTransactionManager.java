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

package io.shardingsphere.transaction.base;

import io.shardingsphere.transaction.base.context.ShardingSQLTransaction;
import io.shardingsphere.transaction.base.saga.ShardingSQLTransactionManager;
import io.shardingsphere.transaction.base.saga.actuator.SagaActuatorFactory;
import io.shardingsphere.transaction.base.saga.actuator.definition.SagaDefinitionFactory;
import io.shardingsphere.transaction.base.saga.config.SagaConfiguration;
import io.shardingsphere.transaction.base.saga.config.SagaConfigurationLoader;
import io.shardingsphere.transaction.base.saga.persistence.SagaPersistenceLoader;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.RecoveryPolicy;
import org.apache.servicecomb.saga.core.application.SagaExecutionComponent;
import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.core.execute.ShardingExecuteDataMap;
import org.apache.shardingsphere.spi.database.DatabaseType;
import org.apache.shardingsphere.transaction.core.ResourceDataSource;
import org.apache.shardingsphere.transaction.core.TransactionOperationType;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.apache.shardingsphere.transaction.spi.ShardingTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Saga Sharding transaction manager.
 *
 * @author yangyi
 * @author zhaojun
 */
public final class SagaShardingTransactionManager implements ShardingTransactionManager {
    
    public static final String SAGA_TRANSACTION_KEY = "saga_transaction";
    
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();
    
    private SagaConfiguration sagaConfiguration;
    
    private SagaExecutionComponent sagaActuator;
    
    private final ShardingSQLTransactionManager shardingSQLTransactionManager = ShardingSQLTransactionManager.getInstance();
    
    public SagaShardingTransactionManager() {
        sagaConfiguration = SagaConfigurationLoader.load();
        PersistentStore sagaPersistence = SagaPersistenceLoader.load(sagaConfiguration.getSagaPersistenceConfiguration());
        sagaActuator = SagaActuatorFactory.newInstance(sagaConfiguration, sagaPersistence);
    }
    
    @Override
    public void init(final DatabaseType databaseType, final Collection<ResourceDataSource> resourceDataSources) {
        for (ResourceDataSource each : resourceDataSources) {
            registerDataSourceMap(each.getOriginalName(), each.getDataSource());
        }
    }
    
    @Override
    public TransactionType getTransactionType() {
        return TransactionType.BASE;
    }
    
    @Override
    public boolean isInTransaction() {
        return shardingSQLTransactionManager.isInTransaction();
    }
    
    @Override
    public Connection getConnection(final String dataSourceName) throws SQLException {
        Connection result = dataSourceMap.get(dataSourceName).getConnection();
        if (isInTransaction()) {
            shardingSQLTransactionManager.getCurrentTransaction().getCachedConnections().put(dataSourceName, result);
        }
        return result;
    }
    
    @Override
    public void begin() {
        if (!shardingSQLTransactionManager.isInTransaction()) {
            shardingSQLTransactionManager.set(new ShardingSQLTransaction());
            ShardingExecuteDataMap.getDataMap().put(SAGA_TRANSACTION_KEY, shardingSQLTransactionManager.getCurrentTransaction());
        }
    }
    
    @Override
    public void commit() {
        if (shardingSQLTransactionManager.isInTransaction() && shardingSQLTransactionManager.getCurrentTransaction().isContainsException()) {
            shardingSQLTransactionManager.getCurrentTransaction().setOperationType(TransactionOperationType.COMMIT);
            sagaActuator.run(SagaDefinitionFactory.newInstance(RecoveryPolicy.SAGA_FORWARD_RECOVERY_POLICY, sagaConfiguration, shardingSQLTransactionManager.getCurrentTransaction()).toJson());
        }
        clearSagaTransaction();
    }
    
    @Override
    public void rollback() {
        if (shardingSQLTransactionManager.isInTransaction()) {
            shardingSQLTransactionManager.getCurrentTransaction().setOperationType(TransactionOperationType.ROLLBACK);
            sagaActuator.run(SagaDefinitionFactory.newInstance(RecoveryPolicy.SAGA_BACKWARD_RECOVERY_POLICY, sagaConfiguration, shardingSQLTransactionManager.getCurrentTransaction()).toJson());
        }
        clearSagaTransaction();
    }
    
    @Override
    public void close() {
        dataSourceMap.clear();
    }
    
    private void registerDataSourceMap(final String datasourceName, final DataSource dataSource) {
        validateDataSourceName(datasourceName);
        dataSourceMap.put(datasourceName, dataSource);
    }
    
    private void validateDataSourceName(final String datasourceName) {
        if (dataSourceMap.containsKey(datasourceName)) {
            throw new ShardingException("datasource {} has registered", datasourceName);
        }
    }
    
    private void clearSagaTransaction() {
        ShardingExecuteDataMap.getDataMap().remove(SAGA_TRANSACTION_KEY);
        ShardingSQLTransactionManager.clear();
    }
}
