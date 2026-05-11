#!/usr/bin/env bash

################################################################################
# Jenkins hook
#
# Copyright (c) 2024 Thales Group - All Rights Reserved.
################################################################################

################################################################################
# Global variables
################################################################################

# Exit code
EXIT_CODE=0

################################################################################
# Main script
################################################################################

# Generate code and build the executable
ant gen exe
[ $? -ne 0 ] && EXIT_CODE=1

# Run the executable.
# The script 'input' starts all the components, lets them run for some time, 
# and quits the application with a 0 return code.
./a.out < input
[ $? -ne 0 ] && EXIT_CODE=1

exit $EXIT_CODE
