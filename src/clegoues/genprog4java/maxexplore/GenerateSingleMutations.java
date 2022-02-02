package clegoues.genprog4java.maxexplore;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.fitness.TestCase;
import clegoues.genprog4java.localization.Localization;
import clegoues.genprog4java.localization.Location;
import clegoues.genprog4java.mut.EditOperation;
import clegoues.genprog4java.mut.WeightedHole;
import clegoues.genprog4java.mut.WeightedMutation;
import clegoues.genprog4java.mut.edits.java.JavaEditFactory;
import clegoues.genprog4java.mut.edits.java.JavaEditOperation;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;
import clegoues.genprog4java.Search.GiveUpException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class GenerateSingleMutations<G extends EditOperation> {
    public JavaEditFactory editFactory = new JavaEditFactory();
    public ArrayList<JavaEditOperation> allMutations;
    public CustomFitness fitnessEngine;
    public Representation<G> rep;
    protected static Logger logger = Logger.getLogger(GenerateSingleMutations.class);

    public static final ConfigurationBuilder.RegistryToken token = ConfigurationBuilder.getToken();


    public GenerateSingleMutations(CustomFitness engine, Representation<G> original) {
        this.fitnessEngine = engine;
        engine.initializeModel();
        try {
            original.getLocalization().reduceSearchSpace();
        } catch (GiveUpException e) {
            e.printStackTrace();
        }
        this.rep = original;
        Fitness.deserializeTestCache();
    }

    public ArrayList<G> evaluate_mutant(int index) {
        Representation<G> variant;
        boolean failed;
        ArrayList<G> genome;
        ArrayList<TestCase> posTests = Fitness.positiveTests;

        JavaEditOperation mut = this.allMutations.get(index);

        // make a new copy of original
        variant = this.rep.copy();
        genome = variant.getGenome();
        genome.add((G) mut);
        variant.setGenome(genome);

        failed = false;
        for (TestCase t : posTests){
            boolean ret = fitnessEngine.mySingleTestCasePass(variant, t);
            // if pass continue, if fail break loop and return that mutant was not neutral.
            if (!ret) {
                t.incrementPatchesKilled();
                Collections.sort(posTests, Collections.reverseOrder());
                failed = true;
                break;
            }
        }
        if (!failed) {
            logger.info("Variant " + variant.getName() + " was neutral");
            return variant.getGenome();
        }
        else {
            logger.info("Variant " + variant.getName() + " was not neutral");
            return null;
        }
    }


    public int evaluate_combination(ArrayList<JavaEditOperation> list) {
        Representation<G> variant;
        ArrayList<G> genome;

        variant = this.rep.copy();
        genome = variant.getGenome();
        for (JavaEditOperation edit : list) {
            genome.add((G) edit);
        }
        variant.setGenome(genome);

        int numPositivePassed = fitnessEngine.myTestPassCount(variant,  true, Fitness.positiveTests);
        if(numPositivePassed < Fitness.positiveTests.size()) {
            logger.info("\t passed " + numPositivePassed + " (adversarially-ordered) positive tests before failing and was not neutral, " + variant.getName()+ " (stored at: " + variant.getVariantFolder() + ")");
            return 0;
        }
        int numNegativePassed = fitnessEngine.myTestPassCount(variant, true, Fitness.negativeTests);
        if(numNegativePassed < Fitness.negativeTests.size()) {
            logger.info("\t passed " + numNegativePassed + " negative tests and was neutral, " + variant.getName()+ " (stored at: " + variant.getVariantFolder() + ")");
            return 1;
        }
        int totalPassed = numNegativePassed + numPositivePassed;
        logger.info("\t passed " + totalPassed + " (ALL) tests and is a repair, " + variant.getName()+ " (stored at: " + variant.getVariantFolder() + ")");
        return 2;
    }

    public void get_mutants(Representation<G> variant) {
        Localization localization = variant.getLocalization();
        ArrayList<Location> faultyAtoms = localization.getFaultLocalization();
        ArrayList<JavaEditOperation> allMuts = new ArrayList<>();

        for(Location loc : faultyAtoms) {
            List<WeightedMutation> availableMutations = variant.availableMutations(loc);
            for(WeightedMutation m : availableMutations) { // Append, Delete, Swap, etc.
                List<WeightedHole> allowed = variant.editSources(loc, m.getLeft()); // sources for append
                for(WeightedHole a: allowed) {
                    JavaEditOperation anEdit = this.editFactory.makeEdit(m.getLeft(), loc, a.getHole());
                    allMuts.add(anEdit);
                }
            }
        }

        this.allMutations = allMuts;
    }

    public ArrayList<JavaEditOperation> stringToMutations(String s) {
        logger.info("the input to stringToMutations is " + s);
        ArrayList<JavaEditOperation> mutations = new ArrayList<>();
        String[] parts = s.split(" ");
        for (String part : parts) {
            for (JavaEditOperation mut : allMutations) {
                if (mut.toString().equals(part)) {
                    mutations.add(mut);
                    break;
                }
            }
        }
        return mutations;
    }



}
