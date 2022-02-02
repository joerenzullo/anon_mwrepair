package clegoues.genprog4java.maxexplore;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.fitness.TestCase;
import clegoues.genprog4java.mut.edits.java.JavaEditOperation;
import clegoues.genprog4java.rep.JavaRepresentation;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;
import clegoues.util.GlobalUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import clegoues.genprog4java.main.Configuration;
import org.apache.log4j.Logger;

import static clegoues.util.ConfigurationBuilder.STRING;

public class OnlineAlgorithm {
    List<Pair<Integer, Double>> bins = new ArrayList<>();
    EnumeratedDistribution<Integer> sampler;
    int max_mutations = 256;
    GenerateSingleMutations<JavaEditOperation> generator;
    int time_limit = 1000;
    int ringwise_depth = 100;
    int ringwise_breadth = 1;
    protected static Logger logger = Logger.getLogger(OnlineAlgorithm.class);
    public static final ConfigurationBuilder.RegistryToken token = ConfigurationBuilder.getToken();

    protected static String oracleGenome = ConfigurationBuilder.of( STRING )
            .withVarName( "oracleGenome" )
            .withDefault( "" )
            .withHelp( "oracle genome" )
            .inGroup( "Search Parameters" )
            .build();

    OnlineAlgorithm(GenerateSingleMutations<JavaEditOperation> generator) {
        this.generator = generator;
    }

    Integer mwua_sample() {
        return this.sampler.sample();
    }

    void mwua_update(Integer bin, Double reward) {
        Pair<Integer, Double> current_info = bins.get(bin);
        Pair<Integer, Double> new_item = new Pair<>(current_info.getFirst(),
                current_info.getSecond() * (1 + reward * (Double) 0.05)); // 0.05 is learning parameter, eta
        bins.set(bin, new_item); // juggling this way because these pairs are immutable
        this.sampler = new EnumeratedDistribution<>(bins); // constructor normalizes to a probability distribution
        this.sampler.reseedRandomGenerator(Configuration.randomizer.nextLong());
    }

    Integer bin_to_sample(Integer bin) {
        int start = (int) Math.pow(2.0, bin);
        int end = (int) Math.pow(2.0, (bin + 1));
        if (this.max_mutations < end) {
            end = this.max_mutations;
        }
        return Configuration.randomizer.nextInt((end-start) + 1) + start;
    }


    Integer sample_to_bin(Integer sample) {
        double logTwo = Math.log(sample) / Math.log(2);
        return (int) Math.round(Math.floor(logTwo));
    }

    void evaluate() {
        int generation = 0;
        int num_mutations;
        int neut_mutations_found;
        int bin;
        int single_mutation_count = generator.allMutations.size();
        ArrayList<JavaEditOperation> mutations_to_apply = new ArrayList<>();
        int cost_this_probe;
        boolean neutrality_fail = false;
        List<String> cached_names = null;
        String considering;

        logger.info("There are " + single_mutation_count + " single mutations in the space.");

        // set bins and weights up initially
        int number_of_bins = sample_to_bin(this.max_mutations);
        for (int i = 0; i < number_of_bins; i++) {
            this.bins.add(new Pair<>(i, Math.exp(-1/Math.pow(2.0, 5.0) * Math.pow(2, i))));
        }
        this.sampler = new EnumeratedDistribution<>(bins);
        this.sampler.reseedRandomGenerator(Configuration.randomizer.nextLong());

        while (generation < this.time_limit) {
            if (neutrality_fail) {
                break;
            }
            if (generation % 10 == 0) {
                generator.fitnessEngine.mySerializeTestCache();
            }
            cost_this_probe = 0;
            mutations_to_apply.clear();
            bin = this.mwua_sample();
            num_mutations = bin_to_sample(bin);
            neut_mutations_found = 0;
            logger.info("Trying to make " + num_mutations + " mutations at once.");
            while (neut_mutations_found < num_mutations) {
                if (generator.fitnessEngine.cached_single_mutations < 3000) {
                    int choice = Configuration.randomizer.nextInt(single_mutation_count);
                    ArrayList<JavaEditOperation> genome = this.generator.evaluate_mutant(choice);
                    if (genome != null) {
                        mutations_to_apply.addAll(genome);
                        neut_mutations_found += 1;
                    }
                    cost_this_probe++;
                    if (cost_this_probe > 1000) {
                        logger.info("Failed to find enough neutral mutations within 1000 tries, quitting online alg.");
                        neutrality_fail = true;
                        break;
                    }
                }
                else {
                    // pick from cached neutral mutations instead
                    if (cached_names == null) {
                        cached_names = new ArrayList<>(CustomFitness.myCache.keySet());
                    }
                    // while mutation is not neutral, resample it
                    considering = cached_names.get(Configuration.randomizer.nextInt(cached_names.size()));
                    while (!generator.fitnessEngine.isCachedNeutral(considering)) {
                        considering = cached_names.get(Configuration.randomizer.nextInt(cached_names.size()));
                    }
                    mutations_to_apply.addAll(generator.stringToMutations(considering));
                    neut_mutations_found += 1;
                }
            }
            if (cost_this_probe == 0) {
                logger.info("This probe contains " + num_mutations + " mutations which were all pulled from cache.");
            }
            else {
                logger.info("This probe contains " + num_mutations + " mutations which took " + cost_this_probe + " trials to identify.");
            }
            int repaired = generator.evaluate_combination(mutations_to_apply);
            if (repaired == 2) {
                logger.info("Repair found! Genome was: " + mutations_to_apply);
                break;
            } else if (repaired == 1) {
                mwua_update(bin, 1.0);
            } else {
                mwua_update(bin, 0.0);
            }
            generation++;
        }
        logger.info("Weight vector: " + bins.toString());
        generator.fitnessEngine.shutdown();
    }

    void oracle() {
        ArrayList<JavaEditOperation> mutations_to_apply = generator.stringToMutations(oracleGenome);
        ArrayList<JavaEditOperation> minimized_set = minimize(mutations_to_apply);
        logger.info("cp " + Configuration.workingDir + "/genprog.log /home/USER_DIRECTORY/minimization/" + Configuration.seed + "/genprog.log");
        GlobalUtils.runCommand("mkdir -p /home/USER_DIRECTORY/minimization/" + Configuration.seed);
        GlobalUtils.runCommand("cp " + Configuration.workingDir + "/genprog.log /home/USER_DIRECTORY/minimization/" + Configuration.seed + "/genprog.log");
    }

    ArrayList<JavaEditOperation> minimize(ArrayList<JavaEditOperation> mutations) {
        int initial_fitness;
        int fitness;
        ArrayList<JavaEditOperation> testing_subset;
        boolean changed = true;
        ArrayList<JavaEditOperation> mutation_set = new ArrayList<>(mutations);
        logger.info("Mutation list: " + mutation_set);
        initial_fitness = generator.evaluate_combination(mutation_set);
        if (initial_fitness != 2) {
            logger.info("Sanity check failed; original program did not evaluate as a repair.");
            return new ArrayList<>();
        }

        while (changed) {
            changed = false;
            for (JavaEditOperation mutation : mutation_set) {
                testing_subset = new ArrayList<>(mutation_set);
                testing_subset.remove(mutation);
                fitness = generator.evaluate_combination(testing_subset);
                if (fitness == initial_fitness) {
                    changed = true;
                    mutation_set = testing_subset;
                    break;
                }
            }
        }
        logger.info("minimized mutation list is " + mutation_set);
        return mutation_set;
    }

    void ringwise() {
        int single_mutation_count = generator.allMutations.size();
        ArrayList<JavaEditOperation> mutations_to_apply = new ArrayList<>();
        ArrayList<Integer> neutral = new ArrayList<>();
        ArrayList<Integer> repairs = new ArrayList<>();
        int index;

        for (int i = 0; i < ringwise_depth; i++)
        {
            neutral.add(0);
            repairs.add(0);
        }

        logger.info("Doing depth first search.");
        for (int breadth = 1; breadth <= ringwise_breadth; breadth++) {
            mutations_to_apply.clear(); // if want path-dependent need to move
            logger.info("At breadth " + breadth);
            for (int depth = 1; depth <= ringwise_depth; depth++) {
                index = depth - 1;
                logger.info("At depth " + depth);
                while (mutations_to_apply.size() < depth) {
                    int choice = Configuration.randomizer.nextInt(single_mutation_count);
                    ArrayList<JavaEditOperation> result_genome = this.generator.evaluate_mutant(choice);
                    if (result_genome != null) {
                        mutations_to_apply.addAll(result_genome);
                    }
                }
                int repaired = generator.evaluate_combination(mutations_to_apply);
                if (repaired == 2) {
                    repairs.set(index, repairs.get(index) + 1);
                    neutral.set(index, neutral.get(index) + 1);
                } else if (repaired == 1) {
                    neutral.set(index, neutral.get(index) + 1);
                }
            }
            generator.fitnessEngine.mySerializeTestCache();
            try {
                FileOutputStream fos = new FileOutputStream("ringwise");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(neutral);
                oos.writeObject(repairs);
                oos.close();
                fos.close();
                logger.info("Just finished breadth " + breadth);
                logger.info("Wrote out mutation accumulation experiment incremental results to 'ringwise'");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        logger.info("neutral array: " + neutral);
        logger.info("repair array: " + repairs);
        try {
            FileOutputStream fos = new FileOutputStream("ringwise");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(neutral);
            oos.writeObject(repairs);
            oos.close();
            fos.close();
            logger.info("Wrote out mutation accumulation experiment final results to 'ringwise'");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        generator.fitnessEngine.shutdown();

    }

}
