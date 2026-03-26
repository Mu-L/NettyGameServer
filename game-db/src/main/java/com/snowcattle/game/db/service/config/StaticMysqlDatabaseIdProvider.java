package com.snowcattle.game.db.service.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 固定返回 mysql，避免 {@link org.apache.ibatis.mapping.VendorDatabaseIdProvider} 在启动时 getConnection() 探测库类型。
 * 供 {@link DbBootConfiguration} 与纯 XML 装配的 {@code SqlSessionFactoryBean} 共用。
 */
public class StaticMysqlDatabaseIdProvider implements DatabaseIdProvider {

    @Override
    public void setProperties(Properties properties) {
    }

    @Override
    public String getDatabaseId(DataSource dataSource) throws SQLException {
        return "mysql";
    }
}
