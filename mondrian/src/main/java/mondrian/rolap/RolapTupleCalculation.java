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


package mondrian.rolap;

import mondrian.calc.Calc;
import mondrian.olap.Util;

import java.util.List;

/**
 * Implementation of {@link mondrian.rolap.RolapCalculation}
 * that changes one or more dimensions, then evaluates a given calculation.
 *
 * <p>It is used to implement sets in slicers, in particular sets of tuples in
 * the slicer.
 *
 * @author jhyde
 * @since May 15, 2009
 */
class RolapTupleCalculation implements RolapCalculation {
    private final List<RolapHierarchy> hierarchyList;
    private final Calc calc;
    private final int hashCode;

    /**
     * Creates a RolapTupleCalculation.
     *
     * @param hierarchyList List of hierarchies to be replaced.
     * @param calc Compiled scalar expression to compute cell
     */
    public RolapTupleCalculation(
        List<RolapHierarchy> hierarchyList,
        Calc calc)
    {
        this.hierarchyList = hierarchyList;
        this.calc = calc;
        this.hashCode = Util.hash(hierarchyList.hashCode(), calc);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RolapTupleCalculation) {
            RolapTupleCalculation calculation = (RolapTupleCalculation) obj;
            return this.hierarchyList.equals(calculation.hierarchyList)
                && this.calc.equals(calculation.calc);
        }
        return false;
    }

    @Override
    public String toString() {
        return calc.toString();
    }

    public void setContextIn(RolapEvaluator evaluator) {
        // Restore default member for each hierarchy
        // in the tuple.
        for (RolapHierarchy hierarchy : hierarchyList) {
            final int ordinal = hierarchy.getOrdinalInCube();
            final RolapMember defaultMember =
                evaluator.root.defaultMembers[ordinal];
            evaluator.setContext(defaultMember);
        }

        evaluator.removeCalculation(this, true);
    }

    public int getSolveOrder() {
        return Integer.MIN_VALUE;
    }

    public int getHierarchyOrdinal() {
        throw new UnsupportedOperationException();
    }

    public Calc getCompiledExpression(RolapEvaluatorRoot root) {
        return calc;
    }

    public boolean containsAggregateFunction() {
        return false;
    }

    public boolean isCalculatedInQuery() {
        return true;
    }
}

// End RolapTupleCalculation.java
