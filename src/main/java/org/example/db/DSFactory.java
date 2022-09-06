package org.example.db;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

public final class DSFactory
{
    private DSFactory()
    {
    }

    public static DataSource create()
    {
        final BasicDataSource source = new BasicDataSource();
        source.setUrl("jdbc:mysql://127.0.0.1:3306");
        source.setUsername("root");
        source.setPassword("");
        source.setDefaultAutoCommit(true);
        source.setLogAbandoned(true);
        source.setTestWhileIdle(true);
        source.setTestOnReturn(true);
        source.setMaxTotal(100);
        source.setMinIdle(0);
        source.setDefaultQueryTimeout(60); // seconds
        source.setPoolPreparedStatements(false);
        source.setMaxOpenPreparedStatements(10);
        source.addConnectionProperty("characterEncoding", "utf8");
        source.addConnectionProperty("useUnicode", "true");
        source.addConnectionProperty("useSSL", "false");
        source.addConnectionProperty("tinyInt1isBit", "false");
        source.addConnectionProperty("zeroDateTimeBehaviour", "convertToNull");
        return source;
    }


}
