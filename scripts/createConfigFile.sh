#!/bin/bash

#This script is called by prepareBug.sh
#The purpose of this script is to set up the environment to run GenProg of a particular defects4j bug.

#Preconditions:
#The variable D4J_HOME should be directed to the folder where defects4j is installed.
#The variable GP4J_HOME should be directed to the folder where genprog4java is installed.

#Output
#Creates a config file

# Example usage, VM:
#./createConfigFile.sh 

if [ "$#" -lt 10 ]; then
    echo "This script should be run with 11 parameters:"
	echo "1st param: LOWERCASEPACKAGE, sentence case (ex: lang, chart, closure, math, time)"
	echo "2nd param: BUGNUMBER (ex: 1,2,3,4,...)"
	echo "3rd param: BUGSFOLDER is the folder where the bug files will be cloned to. Starting from $D4J_HOME"
	echo "4th param: APPROACH is the repair approach to use (e.g., gp, trp, par, all)"
	echo "5th param: DIROFJAVA7 is the folder where the java 7 installation is located"
	echo "6th param: SRCFOLDER"
	echo "7th param: CONFIGLIBS"
	echo "8th param: WD"
	echo "9th param: TESTCP"
	echo "10th param: COMPILECP"
	echo "11th param: ORACLE_GENOME"
	exit 0
fi


LOWERCASEPACKAGE="$1"
BUGNUMBER="$2"
BUGSFOLDER="$3"
APPROACH="$4"
DIROFJAVA7="$5"
SRCFOLDER="$6"
CONFIGLIBS="$7"
WD="$8"
TESTCP="$9"
COMPILECP="${10}"
ORACLE_GENOME="${11}"
BUGWD=$D4J_HOME/$BUGSFOLDER"/"$LOWERCASEPACKAGE"$BUGNUMBER"Buggy

#Add the path of defects4j so the defects4j's commands run 
export PATH=$PATH:"$D4J_HOME"/framework/bin/
export PATH=$PATH:"$D4J_HOME"/framework/util/
export PATH=$PATH:"$D4J_HOME"/major/bin/


#Create config file
FILE=$D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/defects4j.config
/bin/cat <<EOM >$FILE
seed = 0
sanity = yes
popsize = 40
javaVM = $DIROFJAVA7/jre/bin/java
workingDir = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/
outputDir = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/tmp
classSourceFolder = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/$SRCFOLDER
libs = $CONFIGLIBS
sourceDir = $WD
positiveTests = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/pos.tests
negativeTests = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/neg.tests
jacocoPath = $GP4J_HOME/lib/jacocoagent.jar
testClassPath=$TESTCP
srcClassPath=$COMPILECP
compileCommand = $D4J_HOME/$BUGSFOLDER/$LOWERCASEPACKAGE$2Buggy/runCompile.sh
targetClassName = $BUGWD/bugfiles.txt
#class or method
testGranularity=class

# 0.1 for GenProg and 1.0 for TrpAutoRepair and PAR
sample=0.1 

# edits for PAR, GenProg, TrpAutoRepair
# edits=APPEND;DELETE;REPLACE;FUNREP;PARREP;PARADD;PARREM;EXPREP;EXPADD;EXPREM;NULLCHECK;OBJINIT;RANGECHECK;SIZECHECK;CASTCHECK;LBOUNDSET;UBOUNDSET;OFFBYONE;SEQEXCH;CASTERMUT;CASTEEMUT
edits=APPEND;DELETE;REPLACE

# use 1.0,0.1 for TrpAutoRepair and PAR. Use 0.65 and 0.35 for GenProg
negativePathWeight=0.65
positivePathWeight=0.35

# trp for TrpAutoRepair, gp for GenProg and PAR 
search=$APPROACH
oracleGenome = $ORACLE_GENOME

EOM
