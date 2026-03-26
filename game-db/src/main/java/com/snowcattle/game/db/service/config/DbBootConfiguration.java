package com.snowcattle.game.db.service.config;

import com.redis.transaction.service.RGTRedisService;
import com.redis.transaction.service.TransactionServiceImpl;
import com.snowcattle.game.db.sharding.DynamicDataSource;
import com.snowcattle.game.db.sharding.EntityServiceShardingStrategy;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.session.ExecutorType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DbBootConfiguration {

    @Value("${game.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${game.datasource.db0.url}")
    private String db0Url;
    @Value("${game.datasource.db0.username}")
    private String db0Username;
    @Value("${game.datasource.db0.password}")
    private String db0Password;

    @Value("${game.datasource.db1.url}")
    private String db1Url;
    @Value("${game.datasource.db1.username}")
    private String db1Username;
    @Value("${game.datasource.db1.password}")
    private String db1Password;

    @Value("${game.datasource.db2.url}")
    private String db2Url;
    @Value("${game.datasource.db2.username}")
    private String db2Username;
    @Value("${game.datasource.db2.password}")
    private String db2Password;

    @Value("${game.redis.host:127.0.0.1}")
    private String redisHost;
    @Value("${game.redis.port:6379}")
    private int redisPort;
    @Value("${game.redis.timeout-ms:3000}")
    private int redisTimeoutMs;

    @Bean
    public DbConfig dbConfig(
            @Value("${game.db.id:1}") int dbId,
            @Value("${game.db.async-save-worker-size:1}") int asyncSaveWorkerSize,
            @Value("${game.db.async-select-worker-size:1}") int asyncSelectWorkerSize,
            @Value("${game.db.async-operation-package-name:com.snowcattle.game.db}") String asyncOperationPackageName) {
        DbConfig dbConfig = new DbConfig();
        dbConfig.setDbId(dbId);
        dbConfig.setAsyncDbOperationSaveWorkerSize(asyncSaveWorkerSize);
        dbConfig.setAsyncDbOperationSelectWorkerSize(asyncSelectWorkerSize);
        dbConfig.setAsyncOperationPackageName(asyncOperationPackageName);
        return dbConfig;
    }

    @Bean(name = "transactionService")
    public TransactionServiceImpl transactionService() {
        return new TransactionServiceImpl();
    }

    @Bean
    public JedisPoolConfig poolConfig(
            @Value("${game.redis.pool.max-idle:30}") int maxIdle,
            @Value("${game.redis.pool.test-while-idle:true}") boolean testWhileIdle,
            @Value("${game.redis.pool.time-between-eviction-runs-millis:60000}") long evictionRunsMillis,
            @Value("${game.redis.pool.num-tests-per-eviction-run:30}") int testsPerEvictionRun,
            @Value("${game.redis.pool.min-evictable-idle-time-millis:60000}") long minEvictableIdleTimeMillis) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setTimeBetweenEvictionRunsMillis(evictionRunsMillis);
        poolConfig.setNumTestsPerEvictionRun(testsPerEvictionRun);
        poolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        return poolConfig;
    }

    @Bean
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {
        return new JedisPool(poolConfig, redisHost, redisPort, redisTimeoutMs);
    }

    @Bean(name = "rgtRedisService")
    public RGTRedisService rgtRedisService(JedisPool jedisPool) {
        RGTRedisService service = new RGTRedisService();
        service.setJedisPool(jedisPool);
        return service;
    }

    @Bean
    public DataSource jdbc_player_db0() {
        return buildDataSource(db0Url, db0Username, db0Password);
    }

    @Bean
    public DataSource jdbc_player_db1() {
        return buildDataSource(db1Url, db1Username, db1Password);
    }

    @Bean
    public DataSource jdbc_player_db2() {
        return buildDataSource(db2Url, db2Username, db2Password);
    }

    @Bean
    public DynamicDataSource dynamicDataSource(
            DataSource jdbc_player_db0,
            DataSource jdbc_player_db1,
            DataSource jdbc_player_db2) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("jdbc_player_db0", jdbc_player_db0);
        targetDataSources.put("jdbc_player_db1", jdbc_player_db1);
        targetDataSources.put("jdbc_player_db2", jdbc_player_db2);
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(jdbc_player_db0);
        return dynamicDataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dynamicDataSource) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dynamicDataSource);
        sqlSessionFactoryBean.setTypeAliasesPackage("com.snowcattle.game.db.service.jdbc.entity");
        sqlSessionFactoryBean.setTypeHandlersPackage("com.snowcattle.game.db.service.jdbc.handler");
        // 避免默认 VendorDatabaseIdProvider 在启动时 getConnection() 探测库类型（路由键未设置时会踩到空连接）
        sqlSessionFactoryBean.setDatabaseIdProvider(new StaticMysqlDatabaseIdProvider());
        return sqlSessionFactoryBean;
    }

    @Bean
    @Scope("prototype")
    public SqlSessionTemplate sqlSessionTemplate(org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Scope("prototype")
    public SqlSessionTemplate sqlSessionBatchTemplate(org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.snowcattle.game.db.service.jdbc.mapper");
        configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return configurer;
    }

    @Bean
    public EntityServiceShardingStrategy defaultEntityServiceShardingStrategy(
            @Value("${game.db.sharding.db-count:3}") int dbCount,
            @Value("${game.db.sharding.table-count:2}") int tableCount,
            @Value("${game.db.sharding.data-source-prefix:jdbc_player_db}") String dataSourcePrefix) {
        EntityServiceShardingStrategy strategy = new EntityServiceShardingStrategy();
        strategy.setDbCount(dbCount);
        strategy.setTableCount(tableCount);
        strategy.setDataSource(dataSourcePrefix);
        return strategy;
    }

    @Bean
    public EntityServiceShardingStrategy defaultPageEntityServiceShardingStrategy(
            @Value("${game.db.sharding.db-count:3}") int dbCount,
            @Value("${game.db.sharding.table-count:2}") int tableCount,
            @Value("${game.db.sharding.data-source-prefix:jdbc_player_db}") String dataSourcePrefix,
            @Value("${game.db.sharding.page-limit:50}") int pageLimit) {
        EntityServiceShardingStrategy strategy = new EntityServiceShardingStrategy();
        strategy.setDbCount(dbCount);
        strategy.setTableCount(tableCount);
        strategy.setDataSource(dataSourcePrefix);
        strategy.setPageFlag(true);
        strategy.setPageLimit(pageLimit);
        return strategy;
    }

    private DataSource buildDataSource(String url, String username, String password) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaxActive(8);
        dataSource.setMaxWait(1500);
        dataSource.setMaxIdle(2);
        dataSource.setMinIdle(1);
        dataSource.setInitialSize(2);
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(60);
        dataSource.setLogAbandoned(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestWhileIdle(true);
        dataSource.setValidationQuery("select 1");
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setNumTestsPerEvictionRun(10);
        dataSource.setMinEvictableIdleTimeMillis(60000);
        return dataSource;
    }
}
