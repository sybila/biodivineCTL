package cz.muni.fi.ode;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Contradiction;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.modelchecker.ModelAdapter;
import org.antlr.v4.runtime.misc.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NodeFactory implements ModelAdapter<CoordinateNode, ColorFormulae> {


    @NotNull
    private final Set<FloatProposition> revealedPropositions = new HashSet<>();

    private final Map<Long, CoordinateNode> nodeCache = new HashMap<>();
    private final Map<Long, CoordinateNode> borderNodes = new HashMap<>();
    private final OdeModel model;
    @NotNull
    private final HashPartitioner partitioner;
    private final int myId;
    private boolean hasAllNodes = false;
    private StateSpaceGenerator generator;

    public NodeFactory(OdeModel model, @NotNull HashPartitioner partitioner) {
        this.model = model;
        this.partitioner = partitioner;
        this.myId = partitioner.getMyId();
    }

    @NotNull
    public Collection<CoordinateNode> getNodes() {
        return nodeCache.values();
    }

    public synchronized CoordinateNode getNode(@NotNull int[] coordinates) {
        long hash = model.nodeHash(coordinates);
        if (nodeCache.containsKey(hash)) {
            return nodeCache.get(hash);
        } else if (borderNodes.containsKey(hash)) {
            return borderNodes.get(hash);
        } else {
            @NotNull CoordinateNode n = new CoordinateNode(coordinates, hash);
            if (partitioner.getNodeOwner(n) == myId) {
                nodeCache.put(hash, n);
            } else {
                borderNodes.put(hash, n);
            }
            return n;
        }
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, ColorFormulae> predecessorsFor(@NotNull CoordinateNode to, @org.jetbrains.annotations.Nullable @Nullable ColorFormulae borders) {
        if (borders == null) {
            borders = model.getFullColorSet();
        }
        if (!to.hasPredecessorsFor()) {
            Map<CoordinateNode, ColorFormulae> results = generator.getPredecessors(to, model.getFullColorSet());//getNativePredecessors(to.coordinates, model.getFullColorSet(), new HashMap<CoordinateNode, ColorFormulae>());
            to.savePredecessors(results);
        }
        return to.getPredecessors(borders);
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, ColorFormulae> successorsFor(@NotNull CoordinateNode from, @org.jetbrains.annotations.Nullable @Nullable ColorFormulae borders) {
        if (borders == null) {
            borders = model.getFullColorSet();
        }
        return generator.getSuccessors(from, borders); //getNativeSuccessors(from.coordinates, borders, new HashMap<CoordinateNode, ColorFormulae>());
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, ColorFormulae> initialNodes(@NotNull Formula formula) {
        if (formula instanceof Tautology) {
            if (!hasAllNodes) {
                generator.cacheAllNodes();
                hasAllNodes = true;
            }
            @NotNull Map<CoordinateNode, ColorFormulae> results = new HashMap<>();
            for (CoordinateNode node : nodeCache.values()) {
                results.put(node, model.getFullColorSet());
            }
            return results;
        }
        if (formula instanceof Contradiction) {
            return new HashMap<>();
        }
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            @NotNull FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            for (CoordinateNode node : generator.initial(proposition)) {
                //proposition is invariant to parameters
                node.addFormula(proposition, model.getFullColorSet());
            }
            //values are not exclusively our inner nodes, so we can't return them directly.
            //return values;
        }
        @NotNull Map<CoordinateNode, ColorFormulae> results = new HashMap<>();
        for (CoordinateNode node : nodeCache.values()) {
            ColorFormulae validColors = node.getValidColors(formula);
            if (validColors != null && !validColors.isEmpty()) {
                results.put(node, (ColorFormulae) ColorFormulae.createCopy(validColors));
            }
        }
        return results;
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, ColorFormulae> invertNodeSet(@NotNull Map<CoordinateNode, ColorFormulae> nodes) {
        if (!hasAllNodes) {
            generator.cacheAllNodes();
            hasAllNodes = true;
        }
        @NotNull Map<CoordinateNode, ColorFormulae> results = new HashMap<>();
        for (CoordinateNode n : nodeCache.values()) {
            @NotNull ColorFormulae full = model.getFullColorSet();
            ColorFormulae anti = nodes.get(n);
            if (anti != null) {
                full.subtract(anti);
            }
            if (!full.isEmpty()) {
                results.put(n, full);
            }
        }
        return results;
    }

    @Override
    public synchronized boolean addFormula(@NotNull CoordinateNode node, @NotNull Formula formula, @NotNull ColorFormulae parameters) {
        return node.addFormula(formula, parameters);
    }

    @NotNull
    @Override
    public synchronized ColorFormulae validColorsFor(@NotNull CoordinateNode node, @NotNull Formula formula) {
        if (formula instanceof Tautology) return model.getFullColorSet();
        if (formula instanceof Contradiction) return model.getEmptyColorSet();
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            @NotNull FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            for (CoordinateNode n : generator.initial(proposition)) {
                n.addFormula(proposition, model.getFullColorSet());
            }
        }
        ColorFormulae colorSet = node.getValidColors(formula);
        if (colorSet == null) return model.getEmptyColorSet();
        return (ColorFormulae) ColorFormulae.createCopy(colorSet);
    }

    @Override
    public void purge(Formula formula) {
        for (CoordinateNode node : nodeCache.values()) {
            node.purgeFormula(formula);
        }
    }

    public void setGenerator(StateSpaceGenerator generator) {
        this.generator = generator;
    }
}
