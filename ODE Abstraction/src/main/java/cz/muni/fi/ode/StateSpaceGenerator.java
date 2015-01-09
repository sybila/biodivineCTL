package cz.muni.fi.ode;

import com.google.common.collect.Range;

import java.util.*;

/**
 * Created by daemontus on 09/01/15.
 */
public class StateSpaceGenerator {

    private final OdeModel model;
    private final boolean parametrized;
    private final NodeFactory factory;

    public StateSpaceGenerator(OdeModel model, boolean parametrized, NodeFactory factory) {
        this.model = model;
        this.parametrized = parametrized;
        this.factory = factory;
    }

    public Map<CoordinateNode, TreeColorSet> getPredecessors(CoordinateNode from, TreeColorSet borders) {
        return getDirectedEdges(from, borders, false, true);
    }

    public Map<CoordinateNode, TreeColorSet> getSuccessors(CoordinateNode from, TreeColorSet borders) {
        return getDirectedEdges(from, borders, true, true);
    }

    private int paramIndex;
    private double denom;

    private Map<CoordinateNode, TreeColorSet> getDirectedEdges(CoordinateNode from, TreeColorSet border, boolean successors, boolean biggestConvexHullOfParamSubspace) {
        Map<CoordinateNode, TreeColorSet> results = new HashMap<>();

        boolean hasSelfLoop = true;

        for (int v = 0; v < model.variableCount(); v++) {

            boolean lowerPositiveDirection = false;
            boolean lowerNegativeDirection = false;
            boolean upperPositiveDirection = false;
            boolean upperNegativeDirection = false;

            int[][] vertices = getRightVertices(from, v, true);

          /*  System.out.println("Vertices: ");
            for (int[] vertex : vertices) {
                for (int a : vertex) {
                    System.out.print(a + " ");
                }
                System.out.println();
            }*/

            List<Double> paramValues = new ArrayList<>();
            paramIndex = -1;

            double mostRightOneValue = Double.POSITIVE_INFINITY;
            double mostLeftOneValue = Double.NEGATIVE_INFINITY;
            double derivationValue = 0.0;

            // cycle for every vertices in lower (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

//                paramIndex = -1;
                denom = 0.0;

                derivationValue = value(vertex, v);
               // System.out.println("Derivation " + derivationValue);
                if (paramIndex != -1) {

                    if (Math.abs(denom) != 0) {

                        paramValues.add(derivationValue / (-denom) == -0 ? 0 : derivationValue / (-denom));
                        //cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;

                        if (border.get(paramIndex).isEmpty()) {
                            throw new IllegalStateException("Error: no interval for parameter " + paramIndex);
                        }

                        // lowest and highest values of parameter space for chosen variable
                        Range<Double> paramBounds = border.get(paramIndex).span();
                       // System.out.println("Bounds: "+paramBounds.lowerEndpoint()+" "+paramBounds.upperEndpoint());
                        // works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
                        Double lastParamValue = paramValues.get(paramValues.size() - 1);
                        if (denom < 0) {
                            if (!successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerPositiveDirection = true;
                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                    mostRightOneValue = lastParamValue;
                            }
                            if (successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerNegativeDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                    mostLeftOneValue = lastParamValue;
                            }
                        } else { // denom > 0
                            if (successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerNegativeDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                    mostRightOneValue = lastParamValue;
                            }
                            if (!successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerPositiveDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                    mostLeftOneValue = lastParamValue;
                            }
                        }

                    } else {    // abs(denom) == 0 (ERGO: it might be at border of state space)
                        //cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
                        if (derivationValue < 0) {
                            lowerNegativeDirection = true;

                            if (successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        } else {
                            lowerPositiveDirection = true;

                            if (!successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        }
                    }
                } else {    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    //cerr << "derivation = " << derivationValue << endl;
                    if (derivationValue < 0) {
                        lowerNegativeDirection = true;
                    } else {
                        lowerPositiveDirection = true;
                    }
                }

            }


            //cerr << "most left  param value on lower facet: " << mostLeftOneValue << endl;
            //cerr << "most right param value on lower facet: " << mostRightOneValue << endl;

            if(from.coordinates[v] != 0)	{
                if(!successors) {
                    //If I want predecessors of state 's'

                    if(lowerPositiveDirection) {
                        //There exists edge from lower state to state 's'
                        int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[v] = newStateCoors[v] - 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);
                    }
                } else {
                    //If I want successors of state 's'
                    if(lowerNegativeDirection) {
                        //There exists edge from lower state to state 's'

                        int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[v] = newStateCoors[v] - 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);
                    }
                }
            }

            //I want to check upper state in this dimension only if state 's' is not at the top in this dimension

            vertices = getRightVertices(from, v, false);
           /* System.out.println("Vertices: ");
            for (int[] vertex : vertices) {
                for (int a : vertex) {
                    System.out.print(a + " ");
                }
                System.out.println();
            }*/

            paramValues = new ArrayList<>();
            paramIndex = -1;
            mostRightOneValue = Double.POSITIVE_INFINITY;
            mostLeftOneValue = Double.NEGATIVE_INFINITY;
            derivationValue = 0.0;

            // cycle for every vertices in higher (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

//				paramIndex = -1;
                denom = 0.0;

                derivationValue = value(vertex, v);

                if (paramIndex != -1) {

                    if (Math.abs(denom) != 0) {
                        paramValues.add(derivationValue / (denom != 0.0 ? -denom : 1) == -0 ? 0 : derivationValue / (denom != 0.0 ? -denom : 1));
                        //cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;

                        if (border.get(paramIndex).isEmpty()) {
                            throw new IllegalStateException("Error: no interval for parameter " + paramIndex);
                        }

                        // lowest and highest values of parameter space for chosen variable
                        Range<Double> paramBounds = border.get(paramIndex).span();

                        // works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
                        Double lastParamValue = paramValues.get(paramValues.size() - 1);
                        if (denom < 0) {
                            if (successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperPositiveDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                mostRightOneValue = lastParamValue;
                            }
                            if (!successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperNegativeDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                mostLeftOneValue = lastParamValue;
                            }
                        } else { // denom > 0
                            if (!successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperNegativeDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                mostRightOneValue = lastParamValue;
                            }
                            if (successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperPositiveDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                mostLeftOneValue = lastParamValue;
                            }
                        }


                    } else {    // abs(denom) == 0 (ERGO: it might be at border of state space)
                        //	cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
                        if (derivationValue < 0) {
                            upperNegativeDirection = true;

                            if (!successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        } else {
                            upperPositiveDirection = true;

                            if (successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        }
                    }

                } else {    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    //	cerr << "derivation = " << derivationValue << endl;
                    if (derivationValue < 0) {
                        upperNegativeDirection = true;
                    } else {
                        upperPositiveDirection = true;
                    }
                }
            }

            //cerr << "most left  param value on upper facet: " << mostLeftOneValue << endl;
            //cerr << "most right param value on upper facet: " << mostRightOneValue << endl;

            if(from.coordinates[v] != model.getThresholdRanges().get(v).upperEndpoint() - 1) {
                if(!successors) {
                    //If I want predecessors of state 's'

                    if(upperNegativeDirection) {
                        //There exists edge from lower state to state 's'
                        int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[v] = newStateCoors[v] + 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);

                    }
                } else {
                    //If I want successors of state 's'

                    if(upperPositiveDirection) {
                        //There exists edge from lower state to state 's'

                        int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[v] = newStateCoors[v] + 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);

                    }
                }
            }

            if(hasSelfLoop) {
                if(lowerPositiveDirection && upperPositiveDirection && !lowerNegativeDirection && !upperNegativeDirection ||
                        !lowerPositiveDirection && !upperPositiveDirection && lowerNegativeDirection && upperNegativeDirection) {

                    hasSelfLoop = false;
                }
            }
        }

        if(hasSelfLoop) {
            results.put(from, border);
        }

        return results;
    }

    private double value(int[] vertex, int dim) {
        double sum = 0;

        paramIndex = -1;

        List<SumMember> equation = model.getEquationForVariable(dim);
        for (SumMember sumMember : equation) {
            //adding value of constant in actual summember 's' of equation for variable 'dim'
            double underSum = sumMember.getConstant();

            //if(dbg) std::cerr << "\tconstant is " << underSum << "\n";

            //adding values of variables in actual summember 's' of equation for variable 'dim' in given point
            List<Integer> vars = sumMember.getVars();
            for (Integer var : vars) {
                int actualVarIndex = var - 1;
                double thres = model.getThresholdForVarByIndex(actualVarIndex, vertex[actualVarIndex]);
                //if(dbg) std::cerr << "\tthres for var " << actualVarIndex << " is " << thres << "\n";
                underSum *= thres;
            }

            //cerr << "start of evalueting of ramps\n";

            //adding enumerated ramps for actual summember 's' of equation for variable 'dim'
            for (Ramp ramp : sumMember.getRamps()) {
                //cerr << "ramp: " << dataModel.getSumForVarByIndex(dim, s).GetRamps().at(r) << endl;
                int rampVarIndex = ramp.dim - 1;
                //cerr << "ramp var index: " << rampVarIndex << endl;
                double thres = model.getThresholdForVarByIndex(rampVarIndex, vertex[rampVarIndex]);
                //cerr << "thres for this var: " << thres << endl;
                underSum *= ramp.value(thres);
                //cerr << "local underSum = " << underSum << endl;
            }

            //adding enumerated step functions for actual summember 's' of equation for variable 'dim'
            for (Step step : sumMember.getSteps()) {
                int stepVarIndex = step.dim - 1;
                double thres = model.getThresholdForVarByIndex(stepVarIndex, vertex[stepVarIndex]);
                underSum *= step.value(thres);
            }

            //adding average value of actual summember's parameter, if any exists
            if (sumMember.hasParam()) {
                if (!parametrized) {
                    Range<Double> param = model.getParameterRange().get(sumMember.getParam() - 1);
                    underSum *= (param.lowerEndpoint() + param.upperEndpoint()) * 0.5;
                    //adding enumerated summember 's' to sum
                    sum += underSum;
                } else {
                    paramIndex = sumMember.getParam() - 1;
                    denom += underSum;
                }

            } else {
                //adding enumerated summember 's' to sum
                sum += underSum;
            }

        }
        //if ( dbg ) std::cerr << "final value = " << sum << std::endl;
        return sum;
    }

    private int[][] getRightVertices(CoordinateNode from, int dim, boolean lower) {
        List<List<Integer>> vertices = new ArrayList<>();
        List<Integer> coors = new ArrayList<>();

        getRightVerticesRecursive(from, dim, lower, vertices, coors, 0);

        int[][] res = new int[vertices.size()][model.variableCount()];
        for (int i = 0; i < vertices.size(); i++) {
            List<Integer> vertex = vertices.get(i);
            for (int j = 0; j < vertex.size(); j++) {
                res[i][j] = vertex.get(j);
            }
        }
        return res;
    }

    private void getRightVerticesRecursive(CoordinateNode from, int dim, boolean lower, List<List<Integer>> vertices, List<Integer> coors, int actIndex) {
        if (actIndex == model.variableCount()) {
            vertices.add(new ArrayList<>(coors));
            return;
        }

        if (actIndex == dim) {
            if (lower) {
                coors.add(from.coordinates[actIndex]);
            } else {
                coors.add(from.coordinates[actIndex] + 1);
            }
            getRightVerticesRecursive(from, dim, lower, vertices, coors, actIndex+1);
            coors.remove(coors.size() - 1);
        } else {
            for(int i = from.coordinates[actIndex]; i < from.coordinates[actIndex] + 2; ++i) {
                coors.add(i);
                getRightVerticesRecursive(from, dim, lower, vertices, coors, actIndex+1);
                coors.remove(coors.size() - 1);
            }
        }
    }
}