# Design of the ECOA LDP

The LDP is a sub-part of a more complete product from Thales, called SOFTARC™.

Therefore, the design of the LDP is greatly driven by the design of SOFTARC™.

## Technologies

The following technologies are used:

**Java** (11+) is used for the code generators (including model transformations).
The input models are parsed with **[JAXB](https://eclipse-ee4j.github.io/jaxb-ri/4.0.1)**.
For the production of text files (mainly source code), a templating engine, **[StringTemplate](https://github.com/antlr/stringtemplate4)**, is used.

**Ant** is used as a driver for running the code generators. It offers the benefit of being easily extendable with custom user tasks.
For example, some ECOA models can be generated from higer-level models before generation. File copying operations can be automated before or after generation, etc. The file **[Ant/softarc.ant](Ant/softarc.ant)** defines a library of reusable Ant targets and tasks. The root of each ECOA workspace shall contain a file `build.xml`, that shall include it.

## Organisation of files

**`jGenerators/`** contains the code generators, written in Java.

**`MetaModelECOA/`** contains the ECOA metamodel, as defined by the standard. It also contains extensions defined in directory `MetaModelECOA/SOFTARC/`.

**`Ant/`** contains things related to the use of Ant in the LDP context.

**`Examples/`** contains ECOA workspace examples.

## Architecture of the code generators

**TODO**
