package cz.muni.fi.frontend;

import cz.muni.fi.ctl.FormulaNormalizer;
import cz.muni.fi.ctl.FormulaParser;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelChecker;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.MPITokenMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import cz.muni.fi.ode.*;
import mpi.MPI;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

public class Main {
    
    static {
        try {
            System.loadLibrary("generator"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Link error. Using lib from zip file.");
            try {
                NativeUtils.loadLibraryFromJar("/build/binaries/libgenerator.jnilib"); // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                e1.printStackTrace();
                try {
                    NativeUtils.loadLibraryFromJar("/build/binaries/libgenerator.so");
                } catch (IOException e2) {
                    String property = System.getProperty("java.library.path");
                    StringTokenizer parser = new StringTokenizer(property, ";");
                    while (parser.hasMoreTokens()) {
                        System.err.println(parser.nextToken());
                    }
                    System.exit(1);
                }
            }
        }
   }

    public static void main(String[] args) throws InterruptedException {
	    long start = System.currentTimeMillis();
       	System.out.println(System.getProperty( "java.library.path" ));
        MPI.Init(args);



        FormulaParser parser = new FormulaParser();
        FormulaNormalizer normalizer = new FormulaNormalizer();
        try {
            System.out.println("Arg: "+args[args.length - 1]);
            Formula formula = parser.parse(new File(args[args.length - 1]));
            formula = normalizer.normalize(formula);
            System.out.println("Normalized form: "+formula);
            OdeModel model = new OdeModel(args[args.length - 2]);
            model.load();
            RectangularPartitioner partitioner = new RectangularPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
            NodeFactory factory = new NodeFactory(model, partitioner);
            StateSpaceGenerator generator = new StateSpaceGenerator(model, true, factory);
            factory.setGenerator(generator);

            Terminator.TerminatorFactory terminatorFactory = new Terminator.TerminatorFactory(new MPITokenMessenger(MPI.COMM_WORLD));

            TaskMessenger<CoordinateNode, TreeColorSet> taskMessenger = new MpiTaskMessenger(MPI.COMM_WORLD, model.variableCount(), factory, model);

            ModelChecker<CoordinateNode, TreeColorSet> modelChecker = new ModelChecker<>(factory, partitioner, taskMessenger, terminatorFactory);
            modelChecker.verify(formula);
            if (args.length >= 3 && args[args.length - 3].equals("--all")) {
                for (CoordinateNode node : factory.getNodes()) {
                    System.out.println(node.toString());
                }
            } else if (args.length >= 3 && !args[args.length - 3].equals("--none")) {
                for (CoordinateNode node : factory.getNodes()) {
                    TreeColorSet colorSet = factory.validColorsFor(node, formula);
                    if (!colorSet.isEmpty()) {
			            System.out.println(Arrays.toString(node.coordinates)+" "+colorSet);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	int rank = MPI.COMM_WORLD.Rank();
	System.out.println("Waiting for termination "+rank);
        MPI.Finalize();
	System.out.println("Temrinated "+rank);
	System.err.println("Duration: "+(System.currentTimeMillis()
 - start));
	System.exit(0);
    }

}
