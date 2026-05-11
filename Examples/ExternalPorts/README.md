# ExternalPorts


A simple example of use of option COMM PORTS.

## What is this example?

The option **COMM PORTS** in the ECOA AS7 standard allows to communicate between independent applications, 
using standard ECOA mechanisms (events, data, request-response),
and without writing any additional code.

This example illustrates the use of the 'event' communication mechanism on a TCP communication link between an Output port and an Input port.

This example contains 2 ECOA applications:

* a **SENDER** application, containing only one component called SENDER,
* a **RECEIVER** application, containing only one component called RECEIVER.

The SENDER application sends an event (containing a variable-size array of bytes) on an output port.

The RECEIVER application receives an event of the same type on an input port.


## How to use

### Generation and compilation

Check that the environment variable `$SOFTARC_HOME` is set to the LDP installation directory, and that the LDP is built
with `ant build`.

Check the files provided with the example: you can see in this workspace the following files:

  * a type library in `00-Types`, containing simple type definitions,
  * 2 ECOA components in `01-Components`,
  * assembly models,
  * 3 deployment models (one for the SENDER application, one for the RECEIVER application, and one for an "all-in-one" application with the SENDER and RECEIVER components). 
  
Notice that the SENDER and RECEIVER deployments have an **'<external_io>'** section that defines a port (`IN` or `OUT`), containing 
a single operation, `msg`. This operation, wich is an "external operation" of the application, is connected via the assembly to the corresponding operation of the component.

Notice also that there is absolutely *no code specific to communication* (TCP handling or other protocol, serialisation, etc.) in this workspace.

We see that we can deploy the two components:

* either in one single application,
* either in two (independent/isolated/distributed) applications, communicating only through the network,
* without writing any code, only changing a few XML lines.

At the root of the workspace (with directories 00-Types, 01-Components, ...), generate the technical code, and compile it to build an executable file,
for each of the 2 applications: 

    ant all -Ddeploymentname=SENDER
    ant all -Ddeploymentname=RECEIVER

### Execution

Open a new terminal for the RECEIVER, go to its generation/execution directory, copy the provided configuration file for TCP connection details, and run the application (we don't need the interactive console, so we redirect the input to `/dev/null`):

    cd 04-Integration/RECEIVER
    cp ../../external_conf.txt .
    ./a.out </dev/null

The RECEIVER will start and wait for messages.

Do the same for the SENDER:

    cd 04-Integration/SENDER
    cp ../../external_conf.txt .
    ./a.out </dev/null

The SENDER will start, send 4 messages of different sizes, repeat this 4 times, and exit the application.

You can see on the RECEIVER's terminal that it has received all the messages.
The receiver also checks the contents of the received messages (bytes in the array should be: 0, 1, 2, 3, etc..)

## run.sh

See also the provided script `run.sh`; it runs all of the above commands at once.
