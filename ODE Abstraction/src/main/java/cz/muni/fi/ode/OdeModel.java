package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one ODE model
 */
public class OdeModel {

    public final String filename;

    //Do not touch -- used in jni
    @NotNull
    private List<Range<Double>> variableRange = new ArrayList<>();
    @NotNull
    private List<Range<Double>> parameterRange = new ArrayList<>();

    public OdeModel(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);
    }

    private native void cppLoad(String filename);

    @NotNull
    public List<Range<Double>> getVariableRange() {
        return variableRange;
    }

    @NotNull
    public List<Range<Double>> getParameterRange() {
        return parameterRange;
    }

    public TreeColorSet getFullColorSet() {
        TreeColorSet set = TreeColorSet.createEmpty(parameterRange.size());
        for (int i = 0; i < set.size(); i++) {
            set.get(i).add(parameterRange.get(i));
        }
        return set;
    }

    public int variableCount() {
        return variableRange.size();
    }

    public int parameterCount() {
        return parameterRange.size();
    }

}