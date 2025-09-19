# ECOA LDP: Lightweight Development Platform for the ECOA standard

[ECOA] (European Component Oriented Architecture) is an open specification of a software framework 
for component-based mission system software.

ECOA allows to develop _real-time_, _embeddable_ software, in the form of simple, independent and testable _components_.

The Lightweight Development Platform is a simple ECOA platform, implementing the ECOA standard, version [AS7].
It allows anyone to develop, test and run, ECOA components.
It is also a great tool to learn and evaluate ECOA on a ordinary Linux computer or Virtual Machine.

The Lightweight Development Platform is developed by [Thales] Defence Mission Systems.

## Usage guide

See the [Usage guide](USAGE.md) to get started.

## Features and non features

See [FEATURES.md]() for a list of what this ECOA platform supports, and what is does not support.

## Design and architecture

Read the [Design document](DESIGN.md) to understand the design of the LDP.

## Examples

The [Examples]() folder contains examples of LDP use, including an implementation of the philosophers' dinner problem.

## Contributing

Read the [Contribution guide](CONTRIBUTING.md) which outlines our policies, procedures and requirements
for contributing to this project.


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.


## Dependencies

This project uses the following Java libraries, which are redistributed in binary form, and with the following licenses:

* [StringTemplate](https://github.com/antlr/stringtemplate4),
* [ANTLR3](https://www.antlr3.org/),
* [JAXB](https://eclipse-ee4j.github.io/jaxb-ri/4.0.1).

See [jGenerators/lib/README.md](jGenerators/lib/README.md) for instructions on how to download and install Java dependencies.


---

[ECOA]: https://www.ecoa.technology/
[AS7]:  https://www.ecoa.technology/public_draft_specifications.html
[Thales]: https://www.thalesgroup.com/
