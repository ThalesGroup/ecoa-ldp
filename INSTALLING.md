## Prerequisites

* A Linux platform (tested on Fedora 33 and Fedora 41)
    * Root access is not required
* The following software must be available:
    * A Java JRE, version 11 (tested with OpenJDK [](https://openjdk.org) 11)
    * Ant [](https://ant.apache.org/) (tested with version 1.10.8)
    * The `make` build tool
    * The `gcc` compiler (tested with version 10.2.1)

## Installation

The LDP (this repository) may be installed in any directory.

The environment variable `SOFTARC_HOME` shall point to this directory.

## Compiling the LDP

```
cd $SOFTARC_HOME
ant build
```

The LDP is provided in source form, so this must be done at least once, and after any modification of the source code of the LDP.

