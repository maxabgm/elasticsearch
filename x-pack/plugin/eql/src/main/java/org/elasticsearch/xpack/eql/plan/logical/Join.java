/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.eql.plan.logical;

import org.elasticsearch.xpack.eql.EqlIllegalArgumentException;
import org.elasticsearch.xpack.ql.capabilities.Resolvables;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.Expressions;
import org.elasticsearch.xpack.ql.plan.logical.LogicalPlan;
import org.elasticsearch.xpack.ql.tree.NodeInfo;
import org.elasticsearch.xpack.ql.tree.Source;
import org.elasticsearch.xpack.ql.util.Check;
import org.elasticsearch.xpack.ql.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

public class Join extends LogicalPlan {

    private final List<KeyedFilter> queries;
    private final KeyedFilter until;
    private final Attribute timestamp;
    private final Attribute tieBreaker;

    public Join(Source source, List<KeyedFilter> queries, KeyedFilter until, Attribute timestamp, Attribute tieBreaker) {
        super(source, CollectionUtils.combine(queries, until));
        this.queries = queries;
        this.until = until;
        this.timestamp = timestamp;
        this.tieBreaker = tieBreaker;
    }

    private Join(Source source, List<LogicalPlan> queries, LogicalPlan until, Attribute timestamp, Attribute tieBreaker) {
        this(source, asKeyed(queries), asKeyed(until), timestamp, tieBreaker);
    }

    static List<KeyedFilter> asKeyed(List<LogicalPlan> list) {
        List<KeyedFilter> keyed = new ArrayList<>(list.size());

        for (LogicalPlan logicalPlan : list) {
            Check.isTrue(KeyedFilter.class.isInstance(logicalPlan), "Expected a KeyedFilter but received [{}]", logicalPlan);
            keyed.add((KeyedFilter) logicalPlan);
        }

        return keyed;
    }

    static KeyedFilter asKeyed(LogicalPlan plan) {
        Check.isTrue(KeyedFilter.class.isInstance(plan), "Expected a KeyedFilter but received [{}]", plan);
        return (KeyedFilter) plan;
    }

    @Override
    protected NodeInfo<? extends Join> info() {
        return NodeInfo.create(this, Join::new, queries, until, timestamp, tieBreaker);
    }

    @Override
    public Join replaceChildren(List<LogicalPlan> newChildren) {
        if (newChildren.size() < 2) {
            throw new EqlIllegalArgumentException("expected at least [2] children but received [{}]", newChildren.size());
        }
        int lastIndex = newChildren.size() - 1;
        return new Join(source(), newChildren.subList(0, lastIndex), newChildren.get(lastIndex), timestamp, tieBreaker);
    }

    @Override
    public List<Attribute> output() {
        List<Attribute> out = new ArrayList<>();

        out.add(timestamp);
        if (Expressions.isPresent(tieBreaker)) {
            out.add(tieBreaker);
        }

        for (KeyedFilter query : queries) {
            out.addAll(query.output());
        }
        return out;
    }

    @Override
    public boolean expressionsResolved() {
        return timestamp.resolved() && tieBreaker.resolved() && until.resolved() && Resolvables.resolved(queries);
    }

    public List<KeyedFilter> queries() {
        return queries;
    }

    public KeyedFilter until() {
        return until;
    }

    public Attribute timestamp() {
        return timestamp;
    }
    
    public Attribute tieBreaker() {
        return tieBreaker;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, tieBreaker, queries, until);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Join other = (Join) obj;

        return Objects.equals(queries, other.queries)
                && Objects.equals(until, other.until)
                && Objects.equals(timestamp, other.timestamp)
                && Objects.equals(tieBreaker, other.tieBreaker);
    }

    @Override
    public List<Object> nodeProperties() {
        return emptyList();
    }
}