#!/bin/bash

for bug_id in {1..106}
do
  sbatch tmp_experiment.sh Math $bug_id 0 20 maxexplore
done

for bug_id in {1..20}
do
  sbatch tmp_experiment.sh Lang $bug_id 0 20 maxexplore
done

for bug_id in {42..65}
do
  sbatch tmp_experiment.sh Lang $bug_id 0 20 maxexplore
done

for bug_id in {12..27}
do
  sbatch tmp_experiment.sh Time $bug_id 0 20 maxexplore
done

for bug_id in {1..26}
do
  sbatch tmp_experiment.sh Chart $bug_id 0 20 maxexplore
done

for bug_id in {1..105}
do
  sbatch tmp_experiment.sh Closure $bug_id 0 20 maxexplore
done

for bug_id in {107..176}
do
  sbatch tmp_experiment.sh Closure $bug_id 0 20 maxexplore
done
