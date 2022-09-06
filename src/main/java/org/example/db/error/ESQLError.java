package org.example.db.error;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ESQLError
{
    ERROR(0, "08S01"),
    SHUTDOWN_IN_PROGRESS(1053, "08S01"),
    BAD_DB(1049),
    NO_SUCH_TABLE(1146),
    LOCK_DEADLOCK(1213),
    CONNECTION_ERROR(2002),
    CONNECTION_HOST_ERROR(2003);

    static boolean isConnectionException(final SQLException sqle)
    {
        final int errorCode = sqle.getErrorCode();
        final String sqlState = sqle.getSQLState();
        return ERROR.is(errorCode, sqlState) ||
               CONNECTION_ERROR.is(errorCode, sqlState) ||
               CONNECTION_HOST_ERROR.is(errorCode, sqlState) ||
               SHUTDOWN_IN_PROGRESS.is(errorCode, sqlState);
    }

    private final int errorCode;
    private final Set<String> sqlStates;

    ESQLError(final int errorCode, final String... sqlStates)
    {
        this.errorCode = errorCode;
        this.sqlStates = new HashSet<>(Arrays.asList(sqlStates));
    }

    public boolean is(final SQLException sqle)
    {
        return is(sqle.getErrorCode(), sqle.getSQLState());
    }

    public boolean is(final int errorCode, final String sqlState)
    {
        if (this.errorCode != errorCode)
        {
            return false;
        }
        if (!sqlStates.isEmpty())
        {
            return sqlStates.contains(sqlState);
        }
        return true;
    }
}