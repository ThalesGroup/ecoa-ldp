# LDP Features

## Supported environment

**Linux** is the only supported environment for the LDP:

* for the execution of the code generator,
* for the execution of the resulting ECOA application.

The LDP is developed and tested on Linux Fedora 33 on x86-64 architecture.

The LDP should be compatible with most Linux distributions, although this is not
guaranteed and may require some adaptations by the user.

Contributions are welcome for adaptations on Linux variants, and portability enhancements.

**gcc** is the required C compiler, and **make** is required as a build tool.

A **Java** JRE and **Ant** are required to run the generator. See the [user documentation](USAGE.md) for details.


## Supported Standards

* ECOA Architecture Specification, preliminary version AS7
  * Core Standard (except features listed below in _Non-supported elements_)
    * Some of the supported features are:
      * Instanciation of components
      * Component instance properties
      * Component lifecycle states
      * Communication mechanisms: Event, Request-Response, Versioned Data
      * Composite components
      * Sync and async request-responses
      * Immediate and deferred request-responses
      * Notifying Versioned Data
      * Triggers
      * Periodic Trigger Manager
      * External components
      * attribute `fifoSize` in assemblies (see section _FIFO queues sizes_ below )
    * The following features are supported, but experimental:
      * Multiple executables in an application (make single-executable applications if possible: they are easier to run and to debug)
      * WarmStart Context. API is implemented, but nothing is memorized by the infrastructure. 

  * Standard Options
    * OPTION INT64
    * OPTION SUPERVISION (with restrictions; see below)
  * Standard Extensions
    * None

* For the generated technical code: C Language ISO 99 with GNU extensions
  * The LDP supports only the C language.
  * The compiler *gcc* is used with option `-std=gnu99`.

* Bindings
  * ECOA C Language Binding I07
  * SOFTARC C Language Binding I07


## Non-supported elements

The following features are **not** supported presently, but could be added in future evolutions of the LDP:

* "bin-desc": using components delivered in binary form
* OPTION COMM PORTS: application communication ports, external operations (no <external_io> section shall be present in the deployment;
  external operations may appear in the assembly, but they will be ignored)
* OPTION SUPERVISION: supervision of executables is not supported (starting/stopping/querying state/querying names)
* OPTION SUPERVISION: conditional links (elements `<when>` in the assembly) and supervision variables
* OPTION DYNAMIC TRIGGER: Dynamic Trigger Manager 
* OPTION UINT64

The following features are not supported, and they are not considered as consistent with the objectives of the LDP,
so they will probably never be added:

* task priorities
* attribute `uncontrolledAccess` on Versioned Data links (always considered false)
* attributes `activating` and `callbackActivating` in assemblies (always considered true)
* driver components


## Limitations

### Data size limits

* The maximum total size for the parameters of a single operation (event, data, or request-response) is 256 kilobytes.

### FIFO queues sizes

When a component sends an information to another component using an operation  (event, data, or request-response),
the receiving component is not always ready to process it instantaneoulsy, so the information is stored in a queue, waiting to be processed.
The maximum capacity of these queues is defined according to the ECOA Standard by the attributes `fifoSize` in the assembly, for each
operation link.

In the LDP, the logical FIFO queues (defined on an operation link basis) are grouped into physical queues.
There is one physical queue for the all executable, where all the input operations are received. This physical queue is a Unix-domain socket for local interprocess communication.
Therefore, the maximum size corresponds to the size of the socket SARC_socket_out. Its size is, at least, the sum of the size of each operationLink multiplied by 
the size of each fifoSize defined. By default, fifoSize is 8 in each operationLink. However, the size can not exceed the value specified in the file /proc/sys/net/core/wmem_max.
If you want to increase more than this value, you have to modify this file with the command : ```sudo sysctl net.core.wmem_max=X```

### External components

An external component has an additional thread, the external thread, that is specific to the component instance and entirely manage by the user. 
Container functions are _not_ callable from the external thread, except: 
* 1/ access to versioned data operations
* 2/ sending event operations

The container function `stop_external_thread` is not supported. Once started, the external thread of a component cannot be stopped.

### Real-time

_Task priorities_ (attribute `relativePriority` in deployments) are not taken into account. 
The ordinary Linux scheduling policy (SCHED_OTHER) is always used.
No special privilege is needed on the Linux system to run the application.

### Automatic component startup

Components can be initialized and started automatically when the executables start, by using attribute `start_mode` in the deployment.

The default value is `NONE` (no auto start); in this case the components shall be initialized and started by
scripting commands (e.g. `Init *` and `Start *`).

The value `FAST` starts all components in no particular order.

The value `SYNCHRONIZED` is not supported by the LDP and is considered equivalent to `FAST`.


## Checks performed by the LDP

The objective of the LDP is not to be a complete verification tool for the compliance of ECOA models.

Nevertheless, when generating technical code, it performs the following checks:

* XSD validation
* Operations signature compatibilty check: if in the assembly, several operations are connected with a link,
  but their parameters have a different structure, an error is raised and code generation fails.

