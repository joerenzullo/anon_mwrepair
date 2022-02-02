#!/bin/bash

#The purpose of this script is to run Genprog of a particular defects4j bug.

#Preconditions:
#The variable D4J_HOME should be directed to the folder where defects4j is installed.
#The variable GP4J_HOME should be directed to the folder where genprog4java is installed.

#Output
#The output is a folder created in the $D4J_HOME/5thParameter/ where all the variants are stored including the patch, if any was found. 


#Example of usage:
#./runGenProgForBug.sh Math 2 allHuman 100 ExamplesCheckedOut gp 1 5 false /usr/lib/jvm/java-1.7.0-openjdk-amd64 /usr/lib/jvm/java-1.8.0-openjdk-amd64 false \"\" false \"\"

if [ "$#" -lt 15 ]; then
  echo "This script should be run with 15 parameters:"
  echo " 1st param is the project in upper case (ex: Lang, Chart, Closure, Math, Time)"
  echo " 2nd param is the bug number (ex: 1,2,3,4,...)"
  echo " 3th param is the option of running the test suite (ex: allHuman, oneHuman, oneGenerated)"
  echo " 4th param is the test suite sample size (ex: 1, 100)"
  echo " 5th param is the folder where the bug files will be cloned to. Starting from $D4J_HOME (Ex: ExamplesCheckedOut)"
  echo " 6th param is the repair approach to use (e.g., gp, trp, par, all)"
  echo " 7th param is the initial seed. It will then increase the seeds by adding 1 until it gets to the number in the next param."
  echo " 8th param is the final seed."
  echo " 9th param is on if the purpose is to test only fault loc and not really trying to find a patch. When it has reached the end of fault localization it will stop."
  echo " 10th param is the folder where the java 7 installation is located"
  echo " 11th param is the folder where the java 8 installation is located"
  echo " 12th param is set to \"true\" if negative tests are to be specified using sampled tests else set this to \"false\""
  echo " 13th param is the path to file containing sampled negative tests"
  echo " 14th param is set to \"true\" if positive tests are to be specified using sampled tests else set this to \"false\""
  echo " 15th param is the path to file containing sampled positive tests"
  echo " 16th param is the list of mutations to use as the oracle genome"
else
  PROJECT="$1"
  BUGNUMBER="$2"
  OPTION="$3"
  TESTSUITESAMPLE="$4"
  BUGSFOLDER="$5"
  APPROACH="$6"
  STARTSEED="$7"
  UNTILSEED="$8"
  JUSTTESTINGFAULTLOC="$9"
  DIROFJAVA7="${10}"
  DIROFJAVA8="${11}"
  SAMPLENEGTESTS="${12}"
  NEGTESTPATH="${13}"
  SAMPLEPOSTESTS="${14}"
  POSTESTPATH="${15}"
  ORACLE_GENOME="${16}"

  if [ "$#" -ge 15 ]; then
#    DIROFJAVA7="/usr/lib/jvm/java-1.7.0-openjdk"
#    DIROFJAVA8="/usr/lib/jvm/java-1.7.0-openjdk"
    echo "Dir of Java 7 is $DIROFJAVA7"
    echo "Dir of Java 8 is $DIROFJAVA8"
  fi

  if [ -z "$D4J_HOME" ]; then
      echo "Need to set D4J_HOME"
      exit 1
  fi
  if [ -z "$GP4J_HOME" ]; then
      echo "Need to set GP4J_HOME"
      exit 1
  fi

  #This transforms the first parameter to lower case. Ex: lang, chart, closure, math or time
  LOWERCASEPACKAGE=`echo $PROJECT | tr '[:upper:]' '[:lower:]'`

  #Add the path of defects4j so the defects4j's commands run
  export PATH=$PATH:$D4J_HOME/framework/bin

  # directory with the checked out buggy project
  BUGWD=$D4J_HOME/$BUGSFOLDER"/"$LOWERCASEPACKAGE"$BUGNUMBER"Buggy
  export JAVA_HOME=$DIROFJAVA8
  export JRE_HOME=$DIROFJAVA8/jre
  export PATH=$DIROFJAVA8/bin/:$PATH
  #sudo update-java-alternatives -s $DIROFJAVA8

  #Compile Genprog and put the class files in /bin
  #Go to the GenProg folder
  if [ -d "$GP4J_HOME" ]; then
    export JAVA_HOME=$DIROFJAVA7
    export JRE_HOME=$DIROFJAVA7/jre
    export PATH=$DIROFJAVA7/bin/:$PATH

    ./prepareBug.sh $PROJECT $BUGNUMBER $OPTION $TESTSUITESAMPLE $BUGSFOLDER $APPROACH $DIROFJAVA7 $DIROFJAVA8 $SAMPLENEGTESTS $NEGTESTPATH $SAMPLEPOSTESTS $POSTESTPATH "$ORACLE_GENOME"

    # added this to copy in cache files
    # FIXME: genomes do not implement serializable
#    if [ $STARTSEED != 0 ]; then
#      cp $D4J_HOME/"$PROJECT""$BUGNUMBER""$APPROACH"0/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/mytestcache.ser $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/mytestcache.ser
#    fi

    if [ -d "$BUGWD/$WD" ]; then
      #Go to the working directory
      cd $BUGWD/$WD

      for (( seed=$STARTSEED; seed<=$UNTILSEED; seed++ ))
      do
        echo "RUNNING THE BUG: $PROJECT $BUGNUMBER, WITH THE SEED: $seed"

        #Running until fault loc only
        if [ $JUSTTESTINGFAULTLOC == "true" ]; then
          echo "justTestingFaultLoc = true" >> $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/defects4j.config
        fi

        #Changing the seed
        CHANGESEEDCOMMAND="sed -i '1s/.*/seed = $seed/' "$D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/defects4j.config
        eval $CHANGESEEDCOMMAND

        if [ $seed != $STARTSEED ]; then
          REMOVESANITYCOMMAND="sed -i 's/sanity = yes/sanity = no/' "$D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/defects4j.config
          eval $REMOVESANITYCOMMAND

          REMOVEREGENPATHS="sed -i '/regenPaths/d' "$D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/defects4j.config
          eval $REMOVEREGENPATHS
        fi

        export JAVA_HOME=$DIROFJAVA8
        export JRE_HOME=$DIROFJAVA8/jre
        export PATH=$DIROFJAVA8/bin/:$PATH

        JAVALOCATION=$(which java)
        $JAVALOCATION -ea -Dlog4j.configurationFile=file:"$GP4J_HOME"/src/log4j.properties -Dfile.encoding=UTF-8 -classpath "$GP4J_HOME"/target/uber-GenProg4Java-0.0.1-SNAPSHOT.jar clegoues.genprog4java.maxexplore.Main $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/defects4j.config | tee $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/log"$PROJECT""$BUGNUMBER"Seed$seed.txt

        #Save the variants in a tar file
        # tar -cvf variants"$PROJECT""$BUGNUMBER"Seed$seed.tar $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/tmp/
        # mkdir -p $D4J_HOME/done/$PROJECT$BUGNUMBER/$seed
        # mv variants"$PROJECT""$BUGNUMBER"Seed$seed.tar $D4J_HOME/done/$PROJECT$BUGNUMBER/$seed/
        # mv genprog.log $D4J_HOME/done/$PROJECT$BUGNUMBER/$seed/
        # mv mytestcache.ser $D4J_HOME/done/$PROJECT$BUGNUMBER/$seed/

        cd $D4J_HOME && rm -rf $BUGSFOLDER

        # mv $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/tmp/original/ $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/
        # rm -r $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/tmp/
        # mkdir $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/tmp/
        # mv $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/original/ $D4J_HOME/$BUGSFOLDER/"$LOWERCASEPACKAGE""$BUGNUMBER"Buggy/tmp/
      done
    fi
  fi
fi #correct number of params
