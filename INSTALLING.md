## Prerequisites

* A Linux platform (tested on Fedora 33, and Fedora 41)
    * Root access is not required

* The following software must be available:
    * A Java JRE, version 11 (tested with OpenJDK [](https://openjdk.org) 11)
    * [Ant](https://ant.apache.org/), version 1.10.8
      * **Warning**: It is recommended to use this exact version. Newer versions may cause Java compilation errors with JAXB.
        If your Linux distribution does not provide this version of Ant, install it in a directory 
        (for example in `/opt`), and set the `$PATH` environment variable in order to use it.
    * The `make` build tool (tested with version 4.2.1)
    * The `gcc` compiler (tested with version 10.2.1)

Example shell commands (for Fedora; adapt to your distro if needed):

```bash
# Java
dnf install java-11-openjdk

# Ant
wget https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.8-bin.zip
unzip apache-ant-1.10.8-bin.zip -d /opt
export PATH=/opt/apache-ant-1.10.8/bin/:$PATH

# C compilation
dnf install make gcc
```

Installing Ant manually:

```bash
#download Ant 1.10.8 and unzip it somewhere
export PATH=ant_installation_directory/bin:$PATH
ant -version
#shall print someting like:
#Apache Ant(TM) version 1.10.8 compiled on May 10 2020
```

## Installation directory

The LDP (this repository) may be installed in any directory.

The environment variable `SOFTARC_HOME` shall be set to this directory.

## Dependencies

This project uses the following Java libraries, which are redistributed in binary form, and with the following licenses:

* [StringTemplate](https://github.com/antlr/stringtemplate4),
* [ANTLR3](https://www.antlr3.org/),
* [JAXB](https://eclipse-ee4j.github.io/jaxb-ri/4.0.1).

See [jGenerators/lib/README.md](jGenerators/lib/README.md) for instructions on how to download and install Java dependencies.

## Compiling the LDP

```
cd $SOFTARC_HOME
ant build
```

The LDP is provided in source form, so this must be done at least once, and after any modification of the source code of the LDP.

