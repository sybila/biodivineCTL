package cz.muni.fi.modelchecker.verification;

import com.github.daemontus.jafra.Terminator;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.ctl.Op;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import org.jetbrains.annotations.NotNull;

/**
 * Creates new verificator for given formula.
 */
public class FormulaVerificator<N extends Node, C extends ColorSet> {

    @NotNull
    private final StateSpacePartitioner<N> partitioner;
    @NotNull
    private final ModelAdapter<N, C> model;
    @NotNull
    private final TaskMessenger<N, C> taskMessenger;
    @NotNull
    private final Terminator.Factory terminatorFactory;


    public FormulaVerificator(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            @NotNull TaskMessenger<N, C> taskMessenger,
            @NotNull Terminator.Factory terminatorFactory
    ) {
        this.partitioner = partitioner;
        this.model = model;
        this.taskMessenger = taskMessenger;
        this.terminatorFactory = terminatorFactory;
    }

    public void verifyFormula(@NotNull Formula formula) {
        @NotNull Op operator = formula.getOperator();
        FormulaProcessor processor;
        if (operator == Op.NEGATION) {
            processor = new NegationVerificator<>(model, formula, terminatorFactory.createNew());
        } else if(operator == Op.AND) {
            processor = new AndVerificator<>(model, formula, terminatorFactory.createNew());
        } else if(operator == Op.OR) {
            processor = new OrVerificator<>(model, formula, terminatorFactory.createNew());
        } else if(operator == Op.EXISTS_UNTIL) {
            processor = new ExistsUntilVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else if(operator == Op.ALL_UNTIL) {
            processor = new AllUntilVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else if(operator == Op.EXISTS_NEXT) {
            processor = new NextVerificator<>(model, partitioner, formula, terminatorFactory, taskMessenger);
        } else {
            throw new IllegalArgumentException("Cannot verify operator: "+operator);
        }

        processor.verify();
    }
}
