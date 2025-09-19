# Philosophers


A synchronized multiple-client, single server demonstration.

This ECOA application comprises:

* two ECOA component types (Table and Philosopher)
* six component instances (Table and Philosopher[1..5])
* a single protection domain (executable) containing all 6 component instances


## How to use

* Check that the environment variable `$SOFTARC_HOME` is set to the LDP installation directory, and that the LDP is built
with `ant build`.

* At the root of the workspace (with directories 00-Types, 01-Components, ...), generate the technical code, and compile it to build an executable: 

```
ant gen 
ant exe
```

* Run the application:

```
./a.out
```

The program will run indefinitely. It will display the evolution of each philosopher according to his state: starving, eating, thinking...
Stop the execution with `Ctrl-C`.


* Run the application with an input script: 

```
./a.out < input
```

The program will run for 20 seconds before stopping all components (see contents of `input`). 
The duration of the program can be changed in the `input` file on the line `wait`. The time is in milliseconds.
