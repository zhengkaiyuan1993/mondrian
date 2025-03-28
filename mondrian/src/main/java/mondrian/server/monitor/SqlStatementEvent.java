/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package mondrian.server.monitor;

import mondrian.server.Locus;
import mondrian.server.Statement;

/**
 * Event concerning an SQL statement.
 */
public abstract class SqlStatementEvent extends Event {
    /**
     * Identifier of the statement. Unique for the lifetime of the JVM.
     */
    public final long sqlStatementId;

    /**
     * Locus of event. From this you can glean the statement and session.
     */
    public final Locus locus;

    /**
     * SQL text of statement.
     */
    public final String sql;

    /**
     * Purpose of executing this SQL statement.
     */
    public final Purpose purpose;

    /**
     * Creates a SqlStatementEvent.
     *
     * @param timestamp Timestamp
     * @param sqlStatementId SQL statement id
     * @param locus Locus of event
     * @param sql SQL
     * @param purpose Why Mondrian is executing this statement
     */
    public SqlStatementEvent(
        long timestamp,
        long sqlStatementId,
        Locus locus,
        String sql,
        Purpose purpose)
    {
        super(timestamp);
        assert locus != null;
        assert sql != null;
        assert purpose != null;
        this.locus = locus;
        this.sqlStatementId = sqlStatementId;
        this.sql = sql;
        this.purpose = purpose;
    }

    public long getStatementId() {
        if (locus.execution != null) {
            final Statement mondrianStatement =
                locus.execution.getMondrianStatement();
            if (mondrianStatement != null) {
                return mondrianStatement.getId();
            }
        }
        return -1;
    }

    /**
     * Reason why Mondrian is executing this SQL statement.
     */
    public enum Purpose {
        DRILL_THROUGH,
        CELL_SEGMENT,
        TUPLES,
        OTHER
    }
}

// End SqlStatementEvent.java
