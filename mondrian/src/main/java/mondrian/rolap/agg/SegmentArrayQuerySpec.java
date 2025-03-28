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


package mondrian.rolap.agg;

import mondrian.rolap.*;
import mondrian.rolap.sql.SqlQuery;

import java.util.*;

/**
 * Provides the information necessary to generate a SQL statement to
 * retrieve a list of segments.
 *
 * @author jhyde
 * @author Richard M. Emberson
 */
class SegmentArrayQuerySpec extends AbstractQuerySpec {
    private final List<Segment> segments;
    private final Segment segment0;
    private final GroupingSetsList groupingSetsList;

    /*
     * Compound member predicates.
     * Each list constrains one dimension.
     */
    private final List<StarPredicate> compoundPredicateList;

    /**
     * Creates a SegmentArrayQuerySpec.
     *
     * @param groupingSetsList Collection of grouping sets
     * @param compoundPredicateList list of predicates representing the
     * compound member constraints
     */
    SegmentArrayQuerySpec(
        GroupingSetsList groupingSetsList,
        List<StarPredicate> compoundPredicateList)
    {
        super(groupingSetsList.getStar(), false);
        this.segments = groupingSetsList.getDefaultSegments();
        this.segment0 = segments.get(0);
        this.groupingSetsList = groupingSetsList;
        this.compoundPredicateList = compoundPredicateList;
        assert isValid(true);
    }

    /**
     * Returns whether this query specification is valid, or throws if invalid
     * and <code>fail</code> is true.
     *
     * @param fail Whether to throw if invalid
     * @return Whether this query specification is valid
     */
    private boolean isValid(boolean fail) {
        assert segments.size() > 0;
        for (Segment segment : segments) {
            int n = segment.predicates.length;
            if (n != segment0.predicates.length) {
                assert !fail;
                return false;
            }
            for (int j = 0; j < segment.predicates.length; j++) {
                // We only require that the two arrays have the same
                // contents, we but happen to know they are the same array,
                // because we constructed them at the same time.
                if (segment.predicates[j] != segment0.predicates[j]) {
                    assert !fail;
                    return false;
                }
            }
        }
        return true;
    }

    public int getMeasureCount() {
        return segments.size();
    }

    public RolapStar.Measure getMeasure(final int i) {
        return segments.get(i).measure;
    }

    public String getMeasureAlias(final int i) {
        return "m" + Integer.toString(i);
    }

    public RolapStar.Column[] getColumns() {
        return segment0.getColumns();
    }

    /**
     * SqlQuery relies on "c" and index. All this should go into SqlQuery!
     *
     * @see mondrian.rolap.sql.SqlQuery#addOrderBy
     */
    public String getColumnAlias(final int i) {
        return "c" + Integer.toString(i);
    }

    public StarColumnPredicate getColumnPredicate(final int i) {
        return segment0.predicates[i];
    }

    protected List<StarPredicate> getPredicateList() {
        if (compoundPredicateList == null) {
            return super.getPredicateList();
        } else {
            return compoundPredicateList;
        }
    }

    protected void addGroupingFunction(SqlQuery sqlQuery) {
        List<RolapStar.Column> list = groupingSetsList.getRollupColumns();
        for (RolapStar.Column column : list) {
            sqlQuery.addGroupingFunction(column.generateExprString(sqlQuery));
        }
    }

    protected void addGroupingSets(
        SqlQuery sqlQuery,
        Map<String, String> groupingSetsAliases)
    {
        List<RolapStar.Column[]> groupingSetsColumns =
            groupingSetsList.getGroupingSetsColumns();
        for (RolapStar.Column[] groupingSetsColumn : groupingSetsColumns) {
            ArrayList<String> groupingColumnsExpr = new ArrayList<String>();
            for (RolapStar.Column aColumn : groupingSetsColumn) {
                final String columnExpr = aColumn.generateExprString(sqlQuery);
                if (groupingSetsAliases.containsKey(columnExpr)) {
                    groupingColumnsExpr.add(
                        groupingSetsAliases.get(columnExpr));
                } else {
                    groupingColumnsExpr.add(columnExpr);
                }
            }
            sqlQuery.addGroupingSet(groupingColumnsExpr);
        }
    }

    protected boolean isAggregate() {
        return true;
    }
}

// End SegmentArrayQuerySpec.java
