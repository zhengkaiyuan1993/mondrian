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


package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractDoubleCalc;
import mondrian.calc.impl.ValueCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;

/**
 * Definition of the <code>Correlation</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class CorrelationFunDef extends AbstractAggregateFunDef {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Correlation",
            "Correlation(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Returns the correlation of two series evaluated over a set.",
            new String[]{"fnxn", "fnxnn"},
            CorrelationFunDef.class);

    public CorrelationFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc =
            compiler.compileList(call.getArg(0));
        final Calc calc1 =
            compiler.compileScalar(call.getArg(1), true);
        final Calc calc2 =
            call.getArgCount() > 2
            ? compiler.compileScalar(call.getArg(2), true)
            : new ValueCalc(call);
        return new AbstractDoubleCalc(
            call, new Calc[] {listCalc, calc1, calc2})
        {
            public double evaluateDouble(Evaluator evaluator) {
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    TupleList list = evaluateCurrentList(listCalc, evaluator);
                    final double correlation =
                        correlation(
                            evaluator, list, calc1, calc2);
                    return correlation;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            public boolean dependsOn(Hierarchy hierarchy) {
                return anyDependsButFirst(getCalcs(), hierarchy);
            }
        };
    }
}

// End CorrelationFunDef.java
