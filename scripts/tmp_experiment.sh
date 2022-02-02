#!/bin/bash

#SBATCH -N 1                         # number of nodes
#SBATCH -n 20                        # number of cores per node
#SBATCH -t 7-00:00                   # wall time (D-HH:MM)
##SBATCH -A USER_ACCOUNT             # Account hours will be pulled from (commented out with double # in front)
#SBATCH -o %j.out                    # STDOUT (%j = JobId)
#SBATCH -e %j.err                    # STDERR (%j = JobId)
#SBATCH --mail-type=FAIL,END         # Send a notification when the job starts, stops, or fails
#SBATCH --mail-user=USER_EMAIL # send-to address

# project = Defects4J project, e.g. Math
# bugid = Defects4J bug id, e.g. 8
# base_number = this job's index in a set of jobs
# threads = number of cores for this job; should match -n above
# strategy = keyword for maxexplore strategy, e.g. ringwise, online

project="$1"
bugid="$2"
base_number="$3"
threads="$4"
strategy="$5"

# set up
rm -rf /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/
mkdir /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"
cd /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number" || exit
cp /scratch/USER_DIRECTORY/experiment.tar .
tar xf experiment.tar
export D4J_HOME=$PWD/defects4j
export GP4J_HOME=$PWD/maxexplore4java
export PATH=/tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/defects4j/framework/bin:/tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/maven/bin:/tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/java8/bin:$PATH

cd /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/maxexplore4java/scripts || exit

# run experiments
first_number=$((base_number*threads))
last_number=$((base_number*threads+threads))
for ((seed=first_number; seed < last_number; ++seed))
do
  echo "Trying seed $seed on $project $bugid"
  /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/maxexplore4java/scripts/runMaxExploreForBug.sh "$project" "$bugid" allHuman 100 "$project""$bugid""$strategy""$seed" "$strategy" "$seed" "$seed" false /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/java8/ /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/java8/ false \"\" false \"\" &
done
wait

# copy output files
cd /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"/defects4j/done || exit
tar cf "$project""$bugid""$strategy"results"$base_number".tar "$project""$bugid"
mkdir -p /scratch/USER_DIRECTORY/"$strategy"_results
mv "$project""$bugid""$strategy"results"$base_number".tar /scratch/USER_DIRECTORY/"$strategy"_results/

# clean up
rm -rf /tmp/USER_DIRECTORY"$project""$bugid"_"$base_number"