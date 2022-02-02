package clegoues.genprog4java.maxexplore;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.fitness.FitnessValue;
import clegoues.genprog4java.fitness.TestCase;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CustomFitness extends Fitness {
    public static HashMap<String, HashMap<TestCase, FitnessValue>> myCache = new HashMap<>();
    protected static Logger logger = Logger.getLogger(CustomFitness.class);
    public static final ConfigurationBuilder.RegistryToken token = ConfigurationBuilder.getToken();
    public Integer cached_single_mutations = 0;



    public CustomFitness() {
        super();
        myDeserializeTestCache();
    }

    public void shutdown() {
        mySerializeTestCache();
    }

    public void mySerializeTestCache() {
        try {
            FileOutputStream fos = new FileOutputStream("mytestcache.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myCache);
            oos.close();
            fos.close();
            logger.info("Serialized test cache to file mytestcache.ser");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void myDeserializeTestCache(){
        File fl = new File("mytestcache.ser");
        HashMap<String, HashMap<TestCase, FitnessValue>> testCache = null;
        if(fl.isFile()){
            try
            {
                FileInputStream fis = new FileInputStream("mytestcache.ser");
                ObjectInputStream ois = new ObjectInputStream(fis);
                testCache = (HashMap) ois.readObject();
                ois.close();
                fis.close();
            }catch(IOException ioe)
            {
                ioe.printStackTrace();
            }catch(ClassNotFoundException c)
            {
                logger.error("Class not found");
                c.printStackTrace();
            }
            logger.info("Deserialized fitnessCache HashMap from mytestcache.ser");
        } else {
            logger.info("Did not deserialize the fitnessCache HashMap; making a new one");
            testCache = new HashMap<>();
        }
        //System.out.println("hashmap is = " + testCache.entrySet().size() + "  " + testCache.toString());
        myCache.putAll(testCache);
        cached_single_mutations = myCache.size();
    }



    public int myTestPassCount(Representation rep, boolean shortCircuit, List<TestCase> tests) {
        int numPassed = 0;
        for (TestCase thisTest : tests) {
            if (!mySingleTestCasePass(rep, thisTest)) {
                rep.cleanup();
                thisTest.incrementPatchesKilled();
                Collections.sort(tests, Collections.reverseOrder());
                if(shortCircuit) { return numPassed; }
            }
            else { numPassed++; }
        }
        return numPassed;
    }

    public boolean mySingleTestCasePass(Representation rep, TestCase test) {
        HashMap<TestCase, FitnessValue> thisVariantsFitness;
        if(myCache.containsKey(rep.getName())) {
            thisVariantsFitness = myCache.get(rep.getName());
            if (thisVariantsFitness.containsKey(test)) {
                return thisVariantsFitness.get(test).isAllPassed();
            }
        } else {
            thisVariantsFitness = new HashMap<>();
            if (rep.genomeLength() == 1) {
                myCache.put(rep.getName(), thisVariantsFitness);
                cached_single_mutations += 1;
            }
        }
        FitnessValue thisTest = rep.testCase(test);
        thisVariantsFitness.put(test, thisTest);
        return thisTest.isAllPassed();
    }

    public boolean isCachedNeutral(String name) {
        for (TestCase t : Fitness.positiveTests) {
            if (myCache.get(name).containsKey(t)) {
                if (!myCache.get(name).get(t).isAllPassed()) {
                    return false;
                }
            }
        }
        return true;
    }

}
