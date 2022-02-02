package clegoues.genprog4java.maxexplore;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import clegoues.genprog4java.Search.*;
import clegoues.genprog4java.main.Configuration;
import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.localization.DefaultLocalization;
import clegoues.genprog4java.localization.Localization;
import clegoues.genprog4java.localization.UnexpectedCoverageResultException;
import clegoues.genprog4java.mut.edits.java.JavaEditOperation;
import clegoues.genprog4java.rep.CachingRepresentation;
import clegoues.genprog4java.rep.JavaRepresentation;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;
import clegoues.genprog4java.Search.GiveUpException;


public class Main {

    protected static Logger logger = Logger.getLogger(Main.class);


    public static void main(String[] args) {
        Representation original;
        CustomFitness fitnessEngine;
        assert (args.length > 0);
        long startTime = System.currentTimeMillis();
        BasicConfigurator.configure();

        ConfigurationBuilder.register( Configuration.token );
        ConfigurationBuilder.register( Fitness.token );
        ConfigurationBuilder.register( CachingRepresentation.token );
        ConfigurationBuilder.register( JavaRepresentation.token );
        ConfigurationBuilder.register( Population.token );
        ConfigurationBuilder.register( Search.token );
        ConfigurationBuilder.register( OracleSearch.token );
        ConfigurationBuilder.register( RandomSingleEdit.token );
        ConfigurationBuilder.register( DefaultLocalization.token );
        ConfigurationBuilder.register( GenerateSingleMutations.token );
        ConfigurationBuilder.register( CustomFitness.token );
        ConfigurationBuilder.register( OnlineAlgorithm.token );


        ConfigurationBuilder.parseArgs( args );
        Configuration.saveOrLoadTargetFiles();
        ConfigurationBuilder.storeProperties();

        File workDir = new File(Configuration.outputDir);
        if (!workDir.exists())
            workDir.mkdir();
        logger.info("Configuration file loaded");

        fitnessEngine = new CustomFitness();  // Fitness must be created before rep!
        original = new JavaRepresentation();
        try {
            original.load(Configuration.targetClassNames);
        } catch (IOException | UnexpectedCoverageResultException e) {
            e.printStackTrace();
        }
        Localization localization = null;
        try {
            localization = new DefaultLocalization(original);
        } catch (IOException | UnexpectedCoverageResultException e) {
            e.printStackTrace();
        }
        original.setLocalization(localization);

        try {
            original.getLocalization().reduceSearchSpace();
        } catch (GiveUpException e) {
            e.printStackTrace();
        }

        GenerateSingleMutations<JavaEditOperation> searchEngine = new GenerateSingleMutations<JavaEditOperation>(fitnessEngine, original);
        searchEngine.get_mutants(original);

        OnlineAlgorithm online = new OnlineAlgorithm(searchEngine);
        logger.info("The search strategy is " + Search.searchStrategy.trim());

        switch(Search.searchStrategy.trim()) {
            case "ringwise":
                logger.info("Commencing ringwise exploration.");
                online.ringwise();
                break;
            case "minimize":
                logger.info("Commencing oracle genome evaluation (minimize)");
                online.oracle();
                break;
            case "maxexplore":
            default:
                logger.info("Commencing MaxExplore.");
                online.evaluate();
                break;
        }

        int elapsed = getElapsedTime(startTime);
        logger.info("\nTotal elapsed time: " + elapsed);
        Runtime.getRuntime().exit(0);
    }

    private static int getElapsedTime(long start) {
        return (int) (System.currentTimeMillis() - start) / 1000;
    }
}