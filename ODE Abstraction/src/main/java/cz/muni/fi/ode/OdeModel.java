package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.microsoft.z3.Context;
import com.microsoft.z3.RealExpr;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents one ODE model
 */
public class OdeModel {

    private final String filename;

    //Do not touch -- used in jni
    @NotNull
    private final List<Range<Integer>> variableRange = new ArrayList<>();
    @NotNull
    private final List<Range<Double>> parameterRange = new ArrayList<>();
    @NotNull
    private final List<String> variableOrder = new ArrayList<>();
    @NotNull
    private final List<List<Double>> thresholds = new ArrayList<>();
    @NotNull
    private final List<List<SumMember>> equations = new ArrayList<>();

    final List<Range<Integer>> nodeIndexRange = new ArrayList<>();

    //local support data
    private long[] dimensionMultipliers;
    private long stateCount;

    private Context defaultContext = new Context();
    private RealExpr[] contextParameters;

    public OdeModel(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);

        for (Range<Integer> range : variableRange) {
            nodeIndexRange.add(Range.closed(range.lowerEndpoint(), range.upperEndpoint() - 1));
        }

        dimensionMultipliers = new long[getVariableCount()];
        stateCount = 1;
        //count all states and prepare ordering
        for (int i=0; i < getVariableCount(); i++) {
            dimensionMultipliers[i] = stateCount;
            Range<Integer> range = nodeIndexRange.get(i);
            stateCount *= range.upperEndpoint() - range.lowerEndpoint() + 1;
        }
        System.err.println("Multipliers: "+ Arrays.toString(dimensionMultipliers));

        if(parameterCount() > 0) {
            contextParameters = new RealExpr[parameterCount()];
            for(int i = 0; i < parameterCount(); i++) {
                contextParameters[i] = defaultContext.mkRealConst("p" + i);
            }
        } else contextParameters = null;
    }

    public long nodeHash(@NotNull int[] nodeCoordinates) {
        long res = 0;
        for (int i=0; i < dimensionMultipliers.length; i++) {
            res += dimensionMultipliers[i] * nodeCoordinates[i];
        }
        return res;
    }

    public String coordinateString(int[] coordinates) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<coordinates.length; i++) {
            sb
                    .append("[")
                    .append(thresholds.get(i).get(coordinates[i]))
                    .append(",")
                    .append(thresholds.get(i).get(coordinates[i] + 1))
                    .append("]");
            if ( i != coordinates.length - 1 ) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public long getDimensionMultiplier(int dim) {
        return dimensionMultipliers[dim];
    }

    public long getStateCount() {
        return stateCount;
    }

    private native void cppLoad(String filename);

    @NotNull
    public List<Range<Integer>> getThresholdRanges() {
        return variableRange;
    }

    @NotNull
    public List<Range<Double>> getParameterRange() {
        return parameterRange;
    }

    @NotNull
    public TreeColorSet getFullColorSet() {
        //will be changed to create new ColorFormulae with defaultContext as initial parameter and initial constrains

        @NotNull TreeColorSet set = TreeColorSet.createEmpty(parameterRange.size());
        for (int i = 0; i < set.size(); i++) {
            set.get(i).add(parameterRange.get(i));
        }
        return set;
    }

    @NotNull
    public TreeColorSet getEmptyColorSet() {

        // will be changed to ColorFormulae instance with initial defaultContext parameter without constrains
        @NotNull TreeColorSet set = new TreeColorSet();
        //here will be added false assertion to the set
        return set;
    }

    @NotNull
    public Context getDefaultContext() {
        return this.defaultContext;
    }

    @NotNull
    public RealExpr[] getContextParameters() {
        return this.contextParameters;
    }

    @NotNull
    public RealExpr getContextParameter(int index) {
        return this.contextParameters[index];
    }

    public int getVariableIndexByName(String var) {
        for (int i=0; i<variableOrder.size(); i++) {
            if (var.equals(variableOrder.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException(var+" is not a variable of this model. ");
    }

    public String getVariableNameByIndex(int index) {
        return variableOrder.get(index);
    }

    public int getVariableCount() {
        return variableRange.size();
    }

    public int parameterCount() {
        return parameterRange.size();
    }

    public List<SumMember> getEquationForVariable(int dim) {
        return Collections.unmodifiableList(equations.get(dim));
    }

    public int getThresholdCountForVariable(int varIndex) {
        return thresholds.get(varIndex).size();
    }

    public double getThresholdValueForVariableByIndex(int actualVarIndex, int i) {
        return thresholds.get(actualVarIndex).get(i);
    }
}
