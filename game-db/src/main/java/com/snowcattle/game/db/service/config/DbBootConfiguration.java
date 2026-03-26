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
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:game-spring-xml-placeholder.properties", ignoreResourceNotFound = false)
public class DbBootConfiguration {
    private static final String DB_PLACEHOLDER_PATH = "classpath:game-spring-xml-placeholder.properties";
    private static final Pattern DATASOURCE_URL_KEY_PATTERN = Pattern.compile("^game\\.datasource\\.([^.]+)\\.url$");
    private final Properties fallbackProps = new Properties();

    @Value("${game.mybatis.config-location:classpath:mybatis3/sqlMapConfig.xml}")
    private String mybatisConfigLocation;

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
    public DatasourceRegistry datasourceRegistry(@Value("${game.datasource.driver-class-name}") String driverClassName) {
        Map<String, DbNodeProperties> nodes = loadDatasourceNodes();
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Missing required config: game.datasource.<name>.url");
        }
        return new DatasourceRegistry(driverClassName, nodes);
    }

    @Bean
    public RedisProperties redisProperties(
            @Value("${game.redis.host:127.0.0.1}") String host,
            @Value("${game.redis.port:6379}") int port,
            @Value("${game.redis.timeout-ms:3000}") int timeoutMs) {
        RedisProperties p = new RedisProperties();
        p.setHost(host);
        p.setPort(port);
        p.setTimeoutMs(timeoutMs);
        return p;
    }

    @Bean
    public JedisPoolConfig poolConfig(RedisProperties redisProperties) {
        RedisPoolProperties pool = redisProperties.getPool();
        if (pool == null) {
            pool = new RedisPoolProperties();
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(pool.getMaxTotal());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setTestOnBorrow(pool.isTestOnBorrow());
        poolConfig.setTestWhileIdle(pool.isTestWhileIdle());
        poolConfig.setBlockWhenExhausted(pool.isBlockWhenExhausted());
        poolConfig.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
        poolConfig.setNumTestsPerEvictionRun(pool.getNumTestsPerEvictionRun());
        poolConfig.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTimeMillis());
        return poolConfig;
    }

    @Bean
    public JedisPool jedisPool(JedisPoolConfig poolConfig, RedisProperties redisProperties) {
        // 避免运行时依赖 DNS 解析；某些环境下解析 `localhost` 会失败
        String host = redisProperties.getHost();
        if (host == null || host.trim().isEmpty() || "localhost".equalsIgnoreCase(host.trim())) {
            host = "127.0.0.1";
        }
        return new JedisPool(poolConfig, host, redisProperties.getPort(), redisProperties.getTimeoutMs());
    }

    @Bean(name = "rgtRedisService")
    public RGTRedisService rgtRedisService(JedisPool jedisPool) {
        RGTRedisService service = new RGTRedisService();
        service.setJedisPool(jedisPool);
        return service;
    }

    @Bean
    public DynamicDataSource dynamicDataSource(DatasourceRegistry datasourceRegistry) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        String firstKey = null;
        for (Map.Entry<String, DbNodeProperties> entry : datasourceRegistry.getNodes().entrySet()) {
            String dbKey = entry.getKey();
            String dataSourceKey = "jdbc_player_" + dbKey;
            DataSource dataSource = buildDataSource(entry.getValue(), dbKey, datasourceRegistry.getDriverClassName());
            targetDataSources.put(dataSourceKey, dataSource);
            if (firstKey == null) {
                firstKey = dataSourceKey;
            }
        }
        if (firstKey == null) {
            throw new IllegalStateException("No datasource entries loaded");
        }
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(targetDataSources.get(firstKey));
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
        // 从 mybatis 主配置加载 mapper（含 orderMapper.xml）
        String configLocation = mybatisConfigLocation;
        if (configLocation == null || configLocation.trim().isEmpty()) {
            configLocation = "classpath:mybatis3/sqlMapConfig.xml";
        }
        Resource configResource = new DefaultResourceLoader().getResource(configLocation);
        sqlSessionFactoryBean.setConfigLocation(configResource);
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

    private DataSource buildDataSource(DbNodeProperties dbNode, String dbKey, String driverClassName) {
        BasicDataSource dataSource = new BasicDataSource();
        if (dbNode == null) {
            throw new IllegalStateException("Missing required config: game.datasource." + dbKey);
        }
        dataSource.setDriverClassName(requireConfig("game.datasource.driver-class-name", driverClassName));
        dataSource.setUrl(requireConfig("game.datasource." + dbKey + ".url", dbNode.getUrl()));
        dataSource.setUsername(requireConfig("game.datasource." + dbKey + ".username", dbNode.getUsername()));
        dataSource.setPassword(requireConfig("game.datasource." + dbKey + ".password", dbNode.getPassword()));
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

    private String requireConfig(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("Missing required config key name");
        }
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return value.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private Map<String, DbNodeProperties> loadDatasourceNodes() {
        Properties props = loadFallbackProperties();
        Map<String, DbNodeProperties> nodes = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            Matcher matcher = DATASOURCE_URL_KEY_PATTERN.matcher(key);
            if (!matcher.matches()) {
                continue;
            }
            String dbKey = matcher.group(1);
            DbNodeProperties node = new DbNodeProperties();
            node.setUrl(props.getProperty("game.datasource." + dbKey + ".url"));
            node.setUsername(props.getProperty("game.datasource." + dbKey + ".username"));
            node.setPassword(props.getProperty("game.datasource." + dbKey + ".password"));
            nodes.put(dbKey, node);
        }
        return nodes;
    }

    private Properties loadFallbackProperties() {
        if (!fallbackProps.isEmpty()) {
            return fallbackProps;
        }
        try {
            Resource resource = new DefaultResourceLoader().getResource(DB_PLACEHOLDER_PATH);
            try (InputStream in = resource.getInputStream()) {
                fallbackProps.load(in);
            }
            return fallbackProps;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config file: " + DB_PLACEHOLDER_PATH, e);
        }
    }

    public static class DbNodeProperties {
        private String url;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class DatasourceRegistry {
        private final String driverClassName;
        private final Map<String, DbNodeProperties> nodes;

        public DatasourceRegistry(String driverClassName, Map<String, DbNodeProperties> nodes) {
            this.driverClassName = driverClassName;
            this.nodes = nodes;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public Map<String, DbNodeProperties> getNodes() {
            return nodes;
        }
    }

    public static class RedisProperties {
        private String host = "127.0.0.1";
        private int port = 6379;
        private int timeoutMs = 3000;
        private RedisPoolProperties pool = new RedisPoolProperties();

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public RedisPoolProperties getPool() { return pool; }
        public void setPool(RedisPoolProperties pool) { this.pool = pool; }
    }

    public static class RedisPoolProperties {
        private int maxTotal = 50;
        private int maxIdle = 30;
        private boolean testOnBorrow = true;
        private boolean testWhileIdle = true;
        private boolean blockWhenExhausted = true;
        private long timeBetweenEvictionRunsMillis = 60000;
        private int numTestsPerEvictionRun = 30;
        private long minEvictableIdleTimeMillis = 60000;

        public int getMaxTotal() { return maxTotal; }
        public void setMaxTotal(int maxTotal) { this.maxTotal = maxTotal; }
        public int getMaxIdle() { return maxIdle; }
        public void setMaxIdle(int maxIdle) { this.maxIdle = maxIdle; }
        public boolean isTestOnBorrow() { return testOnBorrow; }
        public void setTestOnBorrow(boolean testOnBorrow) { this.testOnBorrow = testOnBorrow; }
        public boolean isTestWhileIdle() { return testWhileIdle; }
        public void setTestWhileIdle(boolean testWhileIdle) { this.testWhileIdle = testWhileIdle; }
        public boolean isBlockWhenExhausted() { return blockWhenExhausted; }
        public void setBlockWhenExhausted(boolean blockWhenExhausted) { this.blockWhenExhausted = blockWhenExhausted; }
        public long getTimeBetweenEvictionRunsMillis() { return timeBetweenEvictionRunsMillis; }
        public void setTimeBetweenEvictionRunsMillis(long v) { this.timeBetweenEvictionRunsMillis = v; }
        public int getNumTestsPerEvictionRun() { return numTestsPerEvictionRun; }
        public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) { this.numTestsPerEvictionRun = numTestsPerEvictionRun; }
        public long getMinEvictableIdleTimeMillis() { return minEvictableIdleTimeMillis; }
        public void setMinEvictableIdleTimeMillis(long v) { this.minEvictableIdleTimeMillis = v; }
    }
}
