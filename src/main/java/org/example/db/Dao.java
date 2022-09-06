package org.example.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.example.db.error.ESQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.example.db.error.ESQLException.wrapSQLException;

public class Dao
{

    public final String schema;
    private final DataSource dataSource;
    private final String table;
    private final Logger logger;

    public Dao(final String schema, final DataSource dataSource)
    {
        this.schema = schema;
        this.dataSource = dataSource;
        this.table = "`" + schema + "`.`table`";
        this.logger = LoggerFactory.getLogger(schema + "[dao]");
    }

    public void init() throws ESQLException
    {
        this.logger.info("Initialising db");
        runDdl(String.format("DROP DATABASE IF EXISTS %s;", this.schema));
        runDdl(String.format("CREATE DATABASE %s;", this.schema));
        runDdl(String.format(
            "CREATE TABLE %s (" +
            " id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            " time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            " tag TEXT NOT NULL" +
            " ) ENGINE=InnoDB;",
            this.table
        ));
        insert("init");
        this.logger.info("Current number of rows is: {}", count());
        this.logger.debug("Initialisation complete");
    }

    public void insert(final String tag) throws ESQLException
    {
        final String baseQuery = String.format("INSERT INTO %s(tag) VALUES(?);", this.table);
        final String fullQuery = baseQuery.replace("?", tag);
        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement pt = connection.prepareStatement(baseQuery)
        )
        {
            logger.trace("Executing: {}", fullQuery);
            try
            {
                pt.setObject(1, tag);
            }
            catch (final SQLException sqle)
            {
                logger.trace("Failed: {}", fullQuery);
                throw wrapSQLException(fullQuery, sqle);
            }
            pt.executeUpdate();
            logger.trace("Finished: {}", fullQuery);
        }
        catch (final SQLException sqle)
        {
            logger.trace("Failed: {}", fullQuery);
            throw wrapSQLException(fullQuery, sqle);
        }
    }

    public int count() throws ESQLException
    {
        final String sql = String.format("SELECT COUNT(*) FROM %s;", this.table);
        try (
            final Connection connection = dataSource.getConnection();
            final Statement st = connection.createStatement()
        )
        {
            logger.trace("Executing: {}", sql);
            final ResultSet resultSet = st.executeQuery(sql);
            if (resultSet.next())
            {
                logger.trace("Finished: {}", sql);
                return resultSet.getInt(1);
            }
            logger.trace("Finished: {}", sql);
            return 0;
        }
        catch (final SQLException sqle)
        {
            logger.trace("Failed: {}", sql);
            throw wrapSQLException(sql, sqle);
        }
    }

    private void runDdl(final String sql) throws ESQLException
    {
        try (
            final Connection connection = dataSource.getConnection();
            final Statement st = connection.createStatement()
        )
        {
            logger.trace("Executing: {}", sql);
            st.executeUpdate(sql);
            logger.trace("Finished: {}", sql);
        }
        catch (final SQLException sqle)
        {
            logger.trace("Failed: {}", sql);
            throw wrapSQLException(sql, sqle);
        }
    }
}
