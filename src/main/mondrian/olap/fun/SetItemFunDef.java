/*
// $Id$
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2006-2011 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractMemberCalc;
import mondrian.calc.impl.AbstractTupleCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.olap.type.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of the <code>&lt;Set&gt;.Item</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>
 * &lt;Set&gt;.Item(&lt;Index&gt;)<br/>
 * &lt;Set&gt;.Item(&lt;String Expression&gt; [, ...])
 * </code></blockquote>
 *
 * @author jhyde
 * @version $Id$
 * @since Mar 23, 2006
 */
class SetItemFunDef extends FunDefBase {
    static final Resolver intResolver =
        new ReflectiveMultiResolver(
            "Item",
            "<Set>.Item(<Index>)",
            "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the zero-based position of the tuple in the set in <Index>.",
            new String[] {"mmxn"},
            SetItemFunDef.class);

    static final Resolver stringResolver =
        new ResolverBase(
            "Item",
            "<Set>.Item(<String> [, ...])",
            "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the member name (or names) in <String>.",
            Syntax.Method)
    {
        public FunDef resolve(
            Exp[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (args.length < 1) {
                return null;
            }
            final Exp setExp = args[0];
            if (!(setExp.getType() instanceof SetType)) {
                return null;
            }
            final SetType setType = (SetType) setExp.getType();
            final int arity = setType.getArity();
            // All args must be strings.
            for (int i = 1; i < args.length; i++) {
                if (!validator.canConvert(
                        i, args[i], Category.String, conversions))
                {
                    return null;
                }
            }
            if (args.length - 1 != arity) {
                throw Util.newError(
                    "Argument count does not match set's cardinality " + arity);
            }
            final int category = arity == 1 ? Category.Member : Category.Tuple;
            FunDef dummy = createDummyFunDef(this, category, args);
            return new SetItemFunDef(dummy);
        }
    };

    public SetItemFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Type getResultType(Validator validator, Exp[] args) {
        SetType setType = (SetType) args[0].getType();
        return setType.getElementType();
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc =
            compiler.compileList(call.getArg(0));
        final Type elementType =
            ((SetType) listCalc.getType()).getElementType();
        final boolean isString =
            call.getArgCount() < 2
            || call.getArg(1).getType() instanceof StringType;
        final IntegerCalc indexCalc;
        final StringCalc[] stringCalcs;
        List<Calc> calcList = new ArrayList<Calc>();
        calcList.add(listCalc);
        if (isString) {
            indexCalc = null;
            stringCalcs = new StringCalc[call.getArgCount() - 1];
            for (int i = 0; i < stringCalcs.length; i++) {
                stringCalcs[i] = compiler.compileString(call.getArg(i + 1));
                calcList.add(stringCalcs[i]);
            }
        } else {
            stringCalcs = null;
            indexCalc = compiler.compileInteger(call.getArg(1));
            calcList.add(indexCalc);
        }
        Calc[] calcs = calcList.toArray(new Calc[calcList.size()]);
        if (elementType instanceof TupleType) {
            final TupleType tupleType = (TupleType) elementType;
            final Member[] nullTuple = makeNullTuple(tupleType);
            if (isString) {
                return new AbstractTupleCalc(call, calcs) {
                    public Member[] evaluateTuple(Evaluator evaluator) {
                        final int savepoint = evaluator.savepoint();
                        evaluator.setNonEmpty(false);
                        final TupleList list =
                            listCalc.evaluateList(evaluator);
                        assert list != null;
                        evaluator.restore(savepoint);
                        String[] results = new String[stringCalcs.length];
                        for (int i = 0; i < stringCalcs.length; i++) {
                            results[i] =
                                stringCalcs[i].evaluateString(evaluator);
                        }
                        listLoop:
                        for (List<Member> members : list) {
                            for (int j = 0; j < results.length; j++) {
                                String result = results[j];
                                final Member member = members.get(j);
                                if (!matchMember(member, result)) {
                                    continue listLoop;
                                }
                            }
                            // All members match. Return the current one.
                            return members.toArray(new Member[members.size()]);
                        }
                        // We use 'null' to represent the null tuple. Don't
                        // know why.
                        return null;
                    }
                };
            } else {
                return new AbstractTupleCalc(call, calcs) {
                    public Member[] evaluateTuple(Evaluator evaluator) {
                        final int savepoint = evaluator.savepoint();
                        evaluator.setNonEmpty(false);
                        final TupleList list =
                            listCalc.evaluateList(evaluator);
                        assert list != null;
                        evaluator.restore(savepoint);
                        final int index = indexCalc.evaluateInteger(evaluator);
                        int listSize = list.size();
                        if (index >= listSize || index < 0) {
                            return nullTuple;
                        } else {
                            final List<Member> members = list.get(index);
                            return members.toArray(new Member[members.size()]);
                        }
                    }
                };
            }
        } else {
            final MemberType memberType = (MemberType) elementType;
            final Member nullMember = makeNullMember(memberType);
            if (isString) {
                return new AbstractMemberCalc(call, calcs) {
                    public Member evaluateMember(Evaluator evaluator) {
                        final int savepoint = evaluator.savepoint();
                        evaluator.setNonEmpty(false);
                        final List<Member> list =
                            listCalc.evaluateList(evaluator).slice(0);
                        assert list != null;
                        evaluator.restore(savepoint);
                        final String result =
                            stringCalcs[0].evaluateString(evaluator);
                        for (Member member : list) {
                            if (matchMember(member, result)) {
                                return member;
                            }
                        }
                        return nullMember;
                    }
                };
            } else {
                return new AbstractMemberCalc(call, calcs) {
                    public Member evaluateMember(Evaluator evaluator) {
                        final int savepoint = evaluator.savepoint();
                        evaluator.setNonEmpty(false);
                        final List<Member> list =
                            listCalc.evaluateList(evaluator).slice(0);
                        assert list != null;
                        evaluator.restore(savepoint);
                        final int index = indexCalc.evaluateInteger(evaluator);
                        int listSize = list.size();
                        if (index >= listSize || index < 0) {
                            return nullMember;
                        } else {
                            return list.get(index);
                        }
                    }
                };
            }
        }
    }

    private static boolean matchMember(final Member member, String name) {
        return member.getName().equals(name);
    }
}

// End SetItemFunDef.java