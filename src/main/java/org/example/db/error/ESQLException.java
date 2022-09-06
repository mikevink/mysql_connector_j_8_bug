package org.example.db.error;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.SQLTransientException;

import org.slf4j.Logger;

import static org.example.db.error.ESQLError.isConnectionException;


public class ESQLException extends SQLException
{
    private static boolean isTimeoutException(final Throwable exception)
    {
        return exception instanceof SQLTransientException;
    }

    public static ESQLException wrapSQLException(final SQLException exception)
    {
        return wrapSQLException(null, exception);
    }

    public static ESQLException wrapSQLException(final String query, final SQLException exception)
    {
        final boolean isConnectionRelated = isConnectionException(exception);
        final boolean isTimeout = isTimeoutException(exception) || (
            exception instanceof BatchUpdateException && isTimeoutException(exception.getCause()));
        return new ESQLException(exception, query, isConnectionRelated, isTimeout);
    }

    public final String query;
    public final boolean isConnectionRelated;
    public final boolean isTimeout;

    public ESQLException(
        final SQLException exception,
        final String query,
        final boolean isConnectionRelated,
        final boolean isTimeout)
    {
        super(exception.getMessage(), exception.getSQLState(), exception.getErrorCode(), exception);
        this.query = query;
        this.isConnectionRelated = isConnectionRelated;
        this.isTimeout = isTimeout;
    }

    @Override
    public String toString()
    {
        return "ESQLException{" +
               "query='" + query + '\'' +
               ", isConnectionRelated=" + isConnectionRelated +
               ", isTimeout=" + isTimeout +
               '}';
    }

    public void log(final Logger logger, final String message)
    {
        logger.error(message, this);
        logger.error("Query: {}", this.query);
        logger.error("SQLState: {}", this.getSQLState());
        logger.error("VendorError: {}", this.getErrorCode());
    }
}
