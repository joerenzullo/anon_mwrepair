This directory contains a modified version of the JARFly implementation of GenProg for Java.

In order to execute the MWRepair search algorithm, a few things are needed:

1. A fully-configured instance of JARFly. See README_JARFLY.md for setup details.

2. An installation of Defects4J. For code and instructions on configuration, see https://github.com/rjust/defects4j

The code that was used to generate the data reported in Evolving Software: Combining Online Learning with Mutation-Based Stochastic Search can be found in the scripts directory. main_run.sh is the top-level script which invokes the others. The scripts are written to be used with the Slurm job scheduler for cluster compute environments.

The results directory contains the modified Defects4J programs that MWRepair identified as plausible or correct repairs (segmented into directories, accordingly).

The full raw data generated by these experiments is in excess of 2TB of program text and run logs. They are available upon request from the authors.
