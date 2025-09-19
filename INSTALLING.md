## Prerequisites

* A Linux platform (tested on Fedora 33 and Fedora 41)
    * Root access is not required
* The following software must be available:
    * A Java JRE, version 11 (tested with OpenJDK [](https://openjdk.org) 11)
    * Ant [](https://ant.apache.org/), version 1.10.8
      * **Warning**: Newer versions may cause Java compilation errors with JAXB.
    * The `make` build tool
    * The `gcc` compiler (tested with version 10.2.1)

## Installation

The LDP (this repository) may be installed in any directory.

The environment variable `SOFTARC_HOME` shall point to this directory.

## Dependencies

See [jGenerators/lib/README.md](jGenerators/lib/README.md) for instructions on how to download and install Java dependencies.

## Compiling the LDP

```
cd $SOFTARC_HOME
ant build
```

The LDP is provided in source form, so this must be done at least once, and after any modification of the source code of the LDP.

