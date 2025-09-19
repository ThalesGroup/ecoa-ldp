/* Copyright (c) 2025 THALES -- All rights reserved */

/**
 * Ce fichier est la définition source du Formalisme, i.e. le modèle de données commun utilisé par toutes les passes de GenSoftarc.
 * 
 * cf README.txt pour les explications.
 * 
 * Rappels:
 * 
 * Types autorisés pour les attributs: 'boolean', 'String', 'long'.
 * '@contains' = relation d'aggrégation/contenance, sinon c'est une simple référence.
 * '[]' = multiple [0..*], sinon cardinalité [0..1]
 *
 */

public class SoftarcFormalismDefinition {

    class Component {
        String xmlID; // unique identifier in the form 'component:{fullName}' or 'library:{fullName}'
        String typeName; // name of the component type
        String implName; // for a component: name of the implementation (local to the component type); for a lib: language
        String fullName; // is equal to '{typeName}/{implName}'

        @contains TypeDefinition[] types;
        @contains Parameter[] attributes;
        @contains PInfo[] pinfos;
        @contains Parameter[] variables;
        @contains ConstantDefinition[] constants;
        @contains OperationEvent[] sentEvents;
        @contains OperationEvent[] receivedEvents;
        @contains OperationData[] data; // concatenation of readData and writtenData, except that R+W operations appear as a W
                              // only
        OperationData[] readData; // contains R operations + W operations for which writeOnly=false
        OperationData[] writtenData;
        @contains OperationRequestResponse[] requiredRequestResponses;
        @contains OperationRequestResponse[] providedRequestResponses;
        @contains OperationEvent[] externalOperations; // TODO for ECOA driver components
        Operation[] operations;
        @contains Trigger[] triggers;
        @contains Topic[] topics;

        String $package; // in C: prefix for all identifiers related to this component (excluding '_'), as defined as 'fullname'
                         // in CI model
        String[] splittedPackage;
        String fileprefix; // the base of filenames related to this component
        String headerextension; // the extension for header files
        String[] sourceextension; // the list of possible extensions for source files; the first one is used for
                                  // generated files
        String implDir; // pathname of the directory containing the CI model
        boolean isExternal; // true if system='EXTERNAL' in CT model
        boolean autoStartExternalThread;
        boolean isTimer; // true if system='TIMERMANAGER' in CT model
        boolean isLibrary; // true if CT model is library.xml
        long stack;
        long externalStack;
        boolean isSupervisor;

        boolean definesListOrMap;
        boolean hasStrings;
        boolean needsValidation;

        String language; // C ADA CPP JAVA PYTHON
        // warning: the following language flags are not computed before pass XRef:
        boolean isCComponent; // true iff language=C
        boolean isCppComponent; // true iff language=CPP
        boolean isAdaComponent; // true iff language=ADA
        boolean isJavaComponent; // true iff language=JAVA
        boolean isPythonComponent; // true iff language=PYTHON
        boolean isRustComponent; // true iff language=PYTHON
        String apiVariant; // = APIType in the implementation model, default=SOFTARC_C
        boolean isEcoa; // true iff apiVariant contains "ECOA"
        boolean hasReset; 
        boolean hasWarmStartContext; 
        boolean needsUTCTime;
        boolean validateData;
        boolean validateParameters;

        Component[] usedLibraries; // directly referenced libraries
        Component[] allLibraries; // directly and indirectly referenced libraries
        Component cComponent; // this is an equivalent component with a C implementation; defined for libraries only
        boolean isPredefLib;
        String jniPackage; // for Java only; =package with "." replaced by "/"
        String[] doc;

        @contains Extra[] compilationFlags;
        @contains Extra[] linkFlags;
        @contains Extra[] additionalJar;
        @contains Extra[] incdir;
        @contains Extra[] srcdir;

        // for Ada implementations:
        String[] extendsGpr;
        String[] withGpr;
        @contains Rename[] gprPackage;

        // for JAVA implementations:
        boolean needsJavaSerialization;
        
        // for RUST
        RustDependency[] rustDependencies;
    }
    
    class RustDependency {
    	String name;
    	String path;
    }

    class TypeDefinition {
        String xmlID; // unique identifier in the form 'type:{typeName}.{parent.language}'
        String typeName; // '{parent.typeName}.{name}', or just '{name}' for predef types
        String name; // non-qualified name of the defined type (unique in the scope of the containing component/library)
        String qName; // name of the defined type in the target language (parent.language)

        String baseTypeName; // name of the base type (for simple, variantrecord, enum, array, fixedarray types)
        TypeDefinition baseType; // reference to the base type (after type resolution). For variant types, this is the type of the
                                 // selector.
        TypeDefinition type; // synonym for baseType, for template compatibility
        String qType; // shortcut for: baseType.qName
        boolean baseTypeIsLocal; // used in Python to determine if prefix is needed

        boolean isPredef;

        boolean isSimple;
        boolean hasUnit;
        String unit;

        boolean isRecord;
        @contains Parameter[] fields;

        boolean isVariantRecord;
        boolean isVariantRecordEnumBased;
        boolean isVariantRecordBooleanBased;        
        String selectName;
        @contains VariantField[] unionFields;
        @contains Parameter defaultUnionField;
        When[] unspecifiedEnumValues;
        String qWhen1; // TODO: à supprimer - replacer dans les templates par: first(t.fields).type.qName

        boolean isEnum;
        boolean isEnumBooleanBased;
        @contains EnumValue[] enumValues;
        EnumValue[] sortedEnumValues;

        boolean isArray;
        boolean isFixedArray;
        String arraySizeOrConstant; // positive integer, or %constant%
        long arraySize;

        boolean isString;
        long length;

        boolean isList;
        boolean isMap;
        String keyTypeName; // name of the key type, for map types
        TypeDefinition keyType; // reference to the key type
        boolean keyTypeIsLocal; // used in Python to determine if prefixc is needed

        TypeDefinition realType; // =baseType, but always non-Simple
        boolean realTypeIsLocal; // used in Python to determine if prefixc is needed
        TypeDefinition cType;
        Component parent;

        boolean needsValidation;
        boolean needsCheck;
        boolean isChar8Array;
        boolean isScalar; // equals to: isPredef or isSimple or isEnum
        boolean isSwitchable; // true if the type can be used in a java switch statement
        boolean isNumeric; // equals to: isPredef or isSimple
        boolean isInteger; // equals to: isScalar && realType!=float && realType!=double
        String minRangeOriginal; // in input syntax, after resolution of constants
        String maxRangeOriginal;
        String minRange; // in BigDecimal syntax
        String maxRange;
        String minRangeValue; // in target language syntax
        String maxRangeValue;
        String minPhysical; // in BigDecimal syntax
        String maxPhysical;
        boolean needsMinCheck; // true if a check is necessary, considering the physical representation of this type in this
                               // langage, e.g. no check needed if min=0 for an uint32 in C
        boolean needsMaxCheck;

        String defaultValue; // used for Java and Python only
        String referenceTypename; // the name of the corresponding Java class; different from qName for predef types only; used
                                  // for Java only
        String cName; // à supprimer; remplacer par .cType.qName // The qualified name of the type, for the C language
                      // implementation of the parent component

        long size; // size of the type when serialized, i.e. useful size only, in bytes
        long alignment;
        long alignedSize;
        long sizeof; // the native maximum size, in bytes; synonym for alignedSize, used in templates

        boolean maySerializeWithCopy; // true if the type may be serialized with a simple memcpy when no byte swapping is
                                      // required.
        boolean isCompact; // true if the type contains no padding bytes, i.e. if serialized size is equal to native size
        boolean isByteOrderNeutral; // true if the type is made only of basic types of one byte

        String description; // Description de la structure du type, pour inclusion dans le Mapping et exploitation par le Panel
        boolean isDocumented;
        String[] doc;
        long hashCodeSeed; // if java

        boolean hasJavaClass; // true if java and has a generated type class
        boolean hasJavaInterface; // true if GenLib-java and has a generated type class
        String rwName; // if hasJavaInterface: ReadWrite{name}; else: {name}
        String rwQName; // if hasJavaInterface: {package}.{rwName}; else: {qName}
        String implName; // if hasJavaInterface: Impl{name}; else: {name}
        String implQName; // if hasJavaInterface: {package}.{implName}; else: {qName}
    }

    class EnumValue {
        String name;
        String qName;
        String valnumOrConstant; // decimal integer (BigInteger format), or %constant%
        long valnum;
        String valnumLitteral; // numeric value in litteral syntax of the target language
        boolean isUserDefined; // si la valeur valnum est imposé par l'utilisateur
        boolean isDocumented;
        String[] doc;
    }

    class Parameter {
        String name;
        String qName;
        String typeName;
        TypeDefinition type;
        String qType; // shortcut for: type.qName
        boolean isDocumented;
        String[] doc;
        boolean isLocal; // used in Python to determine if prefix is needed
    }

    class VariantField extends Parameter {
        String when; // value of the discriminant for which this field exists
        String qWhen; // same as 'when', in target language syntax
    }
    
    class When {
    	String when; // value of the discriminant for which this field exists
        String qWhen; // same as 'when', in target language syntax
    }
    
    class ConstantDefinition {
        String name; // non-qualified name of the defined type (unique in the scope of the containing component/library)
        String qName; // name of the defined type in the target language (parent.language)
        String typeName;
        TypeDefinition type;
        String valueOrConstant;
        String realValue;
        String[] doc;
    }

    class PInfo {
        String name;
        boolean isWritable;
        boolean isDocumented;
        String[] doc;
    }

    abstract class Operation {
        String xmlID; // unique identifier in the form 'op:{Component.fullName}/{name}'
        String name;

        boolean hasInParameters; // input parameters of the operation
        @contains Parameter[] inParameters;
        boolean hasOutParameters; // output parameters of the operation
        @contains Parameter[] outParameters;
        boolean hasParameters; // true iff hasInParameters or outParameters
        boolean hasDocumentedInParameters; // true iff at least one parameter has a non-empty doc
        boolean hasDocumentedOutParameters;
        boolean hasDocumentedParameters;

        @contains DataAccess[] accesses;
        boolean virtual; // virtual events are generated for data notification and triggers
        long size; // size of (input) serialized parameters, including requestID (for requestResponse) and republishFlag (for data)
        boolean isDocumented;
        String[] doc;
        String packedName; // pour Java
    }

    class OperationEvent extends Operation {
        boolean isReceived;
        boolean isSent;
        long repeatTime; // the repeat time for a periodic event, in microseconds
        long delay; // the initial delay for a periodic event, in microseconds
        OperationData dataNotification;
    }

    class OperationData extends Operation {
        TypeDefinition type;
        String typeName;
        boolean isRead;
        boolean isWritten;
        long maxversions;
        boolean writeonly;
        boolean notify;
        boolean activating;
        long fifoSize;
        String qType; // à supprimer; remplacer par .type.qName
    }

    class DataAccess {
        boolean isWrite;
        boolean isWriteFromCurrentVersion;
        OperationData data_action;
    }

    class OperationRequestResponse extends Operation {
        boolean isRequired;
        boolean isProvided;
        boolean isDeferred;
        long maxDeferred;
        boolean isAsynchronous;
        boolean isTimed;
        long timeout; // timeout in 100-nano time units
        long maxRequests;
        long sizeOut; // size of output serialized parameters, including requestID for requestResponse
    }

    class Trigger {
        String name;
        OperationEvent event;
    }

    class Topic {
        String name;
    }

    class Rename {
        String newName;
        String oldName;
    }

    class Extra {
        String value;
        String production;
    }


    class System {
        @contains Component[] components; // component implementations involved in the system, including user-defined libraries,
                                // but not predef libraries; includes stub implementations
        @contains ExternInterface $interface; // special Component representing the interface of the system (i.e., the list of external
                                    // operations), if applicable; has no language, no implementation, no types
        Component[] componentsIncludingInterface; // = 'components' + 'interface'
        @contains Component[] predefLib; // libraries containing predef types; one for each language required
        // pas utilisé: Component[] usedLibraries; // all libraries used directly or indirectly by components and
        // interface, excluding predeflib
        @contains Assembly assembly;
        @contains Mapping mapping;
        TypeDefinition[] allTypes; // all non-predef types, sorted alphabetically on typeName; only one implem of each
                                   // type in this list; computed for Mapping.xml; TODO: move to Mapping
        @contains Registry registry;
    }

    // ---------- ASSEMBLY ------------

    class Assembly {
        @contains Instance[] instances;
        @contains Instance externInstance;
        Instance[] instancesIncludingExtern;
        @contains EventLink[] eventLinks;
        @contains DataLink[] dataLinks;
        @contains RequestResponseLink[] requestResponseLinks;
    }

    class ExternInterface extends Component {
        ExternOperation[] externOperations;
        ExternOperation[] externSentEvents;
        ExternOperation[] externReceivedEvents;
        ExternOperation[] externReadData;
        ExternOperation[] externWrittenData;
        ExternOperation[] externRequiredRequestResponses;
        ExternOperation[] externProvidedRequestResponses;
        TypeDefinition[] externAllTypes; // genlib: all non-predef types, sorted alphabetically on typeName; only one
                                         // implem of each type in this list
    }

    class ExternOperation {
        Operation operation;
        ExternChannel channel;
        ExternChannel responseChannel;
        long externalId;
        OperationLink link;
        boolean isData;
        boolean isEvent;
        boolean isRequestResponse;
        boolean canGenerateInvalidInParameters; // genlib
        boolean canGenerateInvalidOutParameters; // genlib
        boolean canGenerateInvalidInArrays; // genlib
        @contains Parameter[] realInParameters; // genlib
        long genlibId; // genlib
    }

    class ExternChannel extends Channel {
        @contains ExternOperation[] operations;
        ExternOperation[] sortedOperations;
        ExternOperation[] sentEvents;
        ExternOperation[] receivedEvents;
        ExternOperation[] writtenData;
        ExternOperation[] readData;
        ExternOperation[] requiredRequestResponses;
        ExternOperation[] providedRequestResponses;
        ExternOperation[] reveivedEventsAndReadData;
        long channelPoolBlockSize;
        long channelPoolSize;
        long udpPort;
    }

    class Instance {
        String name; // name of the instance, as defined in the AS model
        String id; // identifier usable in C language
        long idNo; // unique identifier of the instance in the system, in range 0..count(instances)-1
        long deadline; // deadline associated to this component, in nanoseconds
        ThreadBase externalThread; // additional thread, if type.isExternal=true
        long bufferSize; // size of buffer used for serialisation of operation parameters
        @contains InstanceAttribute[] attributes;
        @contains InstancePInfo[] pinfos;
        @contains OperationLink[] allLinks;
        OperationLink[] writtenDataLinks;
        OperationLink[] readDataLinks;
        OperationLink[] receivedEventLinks;
        OperationLink[] sentEventLinks;
        OperationLink[] requiredRequestResponsesLinks;
        OperationLink[] providedRequestResponsesLinks;
        @contains OperationGroup[] sortedOperations;
        @contains Variable[] variables; // variables used in whencondition feature (for supervisor components only)
        Executable exec;
        Component type;
        Thread thread;
        Port[] ports;
        @contains PortEvent[] portsEvent;
        @contains PortData[] portsData;
        @contains PortRequestResponse[] portsRequestResponse;
        // LinkedDataAccess[] accesses; // pas utilisé?

        @contains EntryPoint[] entryPoints;
        EntryPoint initialize;
        EntryPoint reset;
        EntryPoint shutdown;
        EntryPoint start;
        EntryPoint stop;
        EntryPoint[] lifecycleEntryPoints;
        EntryPoint[] eventEntryPoints;
        EntryPoint[] requestResponseEntryPoints;
        EntryPoint[] callbackEntryPoints;
        EntryPoint[] directRequestResponseEntryPoints;
        // EntryPoint[] operationEntryPoints;
        // EntryPoint[] actions; // ?

        boolean isExtern; // pseudo-instance Assembly.externInstance, aka "extern" in assembly file
        String packedName;
        boolean hasTrigger;
        @contains TriggerInstance[] triggers;
        @contains Request[] requests;
        long queueSize; // size, in bytes, of the request queue of current instance
        long nbHandledRequests;
        boolean hasAsyncTimeout;
        long instanceVerbosityLevel;
        long topicsVerbosityLevel;
        long observabilityLevel;
        TracePoint tracePoint;

        boolean handleRepublishRequests; // true iff a data written by this instance is read by another
    }

    class InstanceAttribute {
        String name; // name defined in AS model
        Instance parent;
        String value; // value defined in AS model
        String cValue;
        TypeDefinition type;
        String qType; // shortcut for type.qName
        boolean isDocumented;
        String[] doc;
    }

    class InstancePInfo {
        String name; // name defined in CT model
        Instance parent;
        String path; // path defined in AS model
        boolean isDocumented;
        String[] doc;
        String identifier; // identifier defined in CO model
        long id; // defined in CO model
    }

    class Variable {
        String name;
        long id;
        TypeDefinition type;
        String intialValue;
    }

    class EntryPoint {
        String xmlID; // unique identifier in the form 'entrypoint:{instance_name}/{name}'
        String name; // shortcut for event.name
        Port port; // null for lifecycle entrypoints
        Instance instance;
        OperationEvent event;
        OperationRequestResponse requestResponse;
        OperationData data;
        // LinkedDataAccess[] accesses;
        TracePoint observationBegin;
        TracePoint observationEnd;
        boolean checkWCET;
        boolean checkRate;
        boolean abortable;
        long wcet; // in nanoseconds
        @contains Rate highestRate;
        @contains WastedTime wasteTimeBefore;
        @contains WastedTime wasteTimeUntil;
        boolean wasteTimeUntilWCET;
        boolean wasteTimeUntilWCETisRandomized;
    }

    class Rate {
        long numberOfOccurences;
        long timeFrame; // in nanoseconds
    }

    class WastedTime {
        long nanoseconds;
        boolean isRandom;
    }

    abstract class Port {
        String xmlID; // unique identifier in the form 'port:instance.name/operation.name'
        Instance instance;
        Operation operation;
        TracePoint observation;
    }

    class PortData extends Port {
        OperationData data;
        DataLink reference;
        TracePoint observationRead;
    }

    class PortEvent extends Port {
        OperationEvent event;
    }

    class PortRequestResponse extends Port {
        OperationRequestResponse requestResponse;
    }

    class DataLinkElement {
        PortData port;
        Instance[] whenkos;
        @contains WhenCondition[] whenconditions;
    }

    class EventLinkSender {
        PortEvent port;
        Instance[] whenkos;
        @contains WhenCondition[] whenconditions;
    }

    class EventLinkReceiver extends EventLinkSender {
        long fifoSize;
        boolean activating;
    }

    class RequestResponseLinkReceiver {
        PortRequestResponse port;
        long fifoSize;
        boolean activating;
        String inChannelName;
        String outChannelName;
        Instance[] whenkos;
        @contains WhenCondition[] whenconditions;
    }

    abstract class Link {
        long id;
        boolean includeExtern;
    }

    class EventLink extends Link {
        @contains EventLinkSender[] senders;
        @contains EventLinkReceiver[] receivers;
        DataLink notifiedData;
    }

    class DataLink extends Link {
        @contains DataLinkElement[] writers;
        @contains DataLinkElement[] readers;
        boolean direct; // attribute defined in AS model
        String defaultValue; // in softarc syntax
        TypeDefinition defaultValueType; // type used to parse default value in softarc syntax
        ThreadBase[] threadsLinked;
    }

    class RequestResponseLink extends Link {
        @contains RequestResponseLinkReceiver client;
        @contains RequestResponseLinkReceiver[] servers;
    }

    // ---------- MAPPING ------------

    class Mapping {
        String deploymentName;
        boolean isLittleEndian;
        boolean isSupervisedSystem;
        boolean autoStart;
        boolean fastStart;
        boolean safeReaders;
        boolean initializeOutput;
        boolean simulation;
        boolean generateObservability;
        boolean checkWCET;
        boolean checkRate;
        boolean realtimeMonitoringMaySuppressActivations;
        boolean buildWithAnt;
        boolean buildWithMake;
        boolean buildWithGPR;

        long maxPlatformId; // highest value of 'id' in 'platforms'
        long maxThreadId; // highest value of 'id' in 'executables.threads'
        long nbPlatforms; // size of list 'platforms'
        long nbExecutables; // size of list 'executables'
        long nbInstances; // size of list 'instances'
        long nbOffer;
        @contains Platform[] platforms;
        ExternChannel[] inChannels;
        ExternChannel[] outChannels;
        ExternChannel[] inOutChannels;
        @contains Executable[] executables;
        @contains Executable globalExecutable; // LDP only
        @contains TracePoint[] tracePoints;
        TracePoint softarcRuntimeTracePoint;
    }

    class Registry {
        long observationIdOffset; // offset between Variables and TracePoints identifiers: <Variable.id> = observationIdOffset +
                                 // <TracePoint.id>
        Long[] initialValues;
    }

    class Platform {
        String xmlID; // unique identifier in the form 'platform:name'
        String name; // name defined in DE model
        long idNo; // identifier in [0..N-1], where N is the number of platforms
        String id; // text identifier 'PLATFORM_{uppercase(name)}'
        String address;
        String panel;
        long sockmin;
        long sockmax;
        long panelCommand;
        long panelAnswer;
        long trace;
        long display;
        boolean traceOnStderr;
        boolean displayOnStdout;
        String production;
        String makespec;
        String family;
        @contains OsProperties osProperties;
        @contains ProductionInfo productionInfo;

        long maxNbThreads; // max number of threads per executable for this platform
        long maxNbChannels; // max number of channels per executable for this platform
        boolean needsByteSwapping; // true if bytes need to be swapped between machine representation and error representation
        Executable[] executables;
        @contains DataVersion[] dataVersions;
        @contains Initialization initialization;
        boolean hasDefaultValues; // true if at least one DataVersion in dataVersions has a default value
        Long[] capacities; // allowed priority levels, as defined by capacities/priority/@value or @priomin and
                              // @priomax in DE model
        Instance[] pfCompInstances;
        Executable dispatcher;
        Executable[] executablesButDispatchers;
        long parameterSize; // size, in bytes, of the shared memory dedicated to storage of incoming requests parameters
    }

    class Executable {
        String name; // name, as defined in DE model
        String id; // identifier usable in C language
        long idNo; // identifier in [0..N-1], where N is the number of executables in the platform
        long nbOffer; // count of routes associated to written data operations and sent event operations
        long nbDispatchedOperations; // number of operations which may be dispatched
        boolean isTimerManagerContainer; // true if it contains at least a timer manager component
        boolean isExternalComponentContainer;
        boolean isSupervisorComponentContainer;
        @contains Container[] containers;
        @contains Thread[] threads;
        @contains ThreadBase[] mwThreads;
        DataVersion[] dataVersions;
        @contains Dispatch[] dispatches;
        ThreadBase[] allThreads; // threads+mwThreads+dispatches (computed by GenConfig)
        @contains Route[] routesForData;
        @contains Route[] routesForEvents;
        @contains Route[] routesForRequests;
        @contains Route[] routesForResponses;
        Platform parent;
        EventLink[] sentEventsLinks;
        // OperationLink[] receivedEventLinks;
        RequestResponseLink[] requiredRequestResponsesLinks;
        RequestResponseLink[] providedRequestResponsesLinks;
        DataLink[] writtenDataLinks;
        // OperationLink[] readDataLinks;
        Component[] componentTypes;
        Component[] pythonComponents;
        Instance[] instances;
        @contains Channel[] channels;
        boolean isDispatcher;
        Component[] dispatcherRequiredComponentTypes; // TODO: rename to ...Libraries
        boolean hasAdaInstance;
        boolean hasCInstance;
        boolean hasCppInstance;
        boolean hasJavaInstance;
        boolean hasPythonInstance;
        boolean hasRustInstance;
        boolean hasTriggers;
        long numberOfTriggers;
        Instance[] triggersInstances;
        String sourceDirectory;
        boolean hasAlarms;
        long numberOfAlarms;
        String packedName;
        String command; // command to launch an executable from the dispatcher
        String basedir; // base directory for building the executable
        boolean buildWithMake;
        boolean buildWithNinja;
        boolean buildWithGPR;
        boolean buildGlobalMode;
        boolean isRustDebug;
        String rustBuildDirectory;
        boolean isRustTargetSpecified;
        String rustTarget;
        @contains SrcCollection mainC;
        @contains SrcCollection mainCPP;
        @contains SrcCollection mainADA;
        @contains SrcCollection mainJAVA;
        @contains SrcCollection[] sources;
        String[] linkflags;
        String[] additionaljar;
        String execfilename;
        boolean needJniDefFile;
        boolean unsupported;
        String mainsrcfile;
        String mainobjfile;
        boolean tryMemoryLock;
        long maxBufferOut; // sum of each MaxBufferIn of each task in the exec.
    }

    class SrcCollection {
        // A set of source files, sharing the same language and the same compilation options.
        // Can correspond to a SOFTARC component/library implementation, or the "main" generated code of an executable.
        String name;
        SrcCollection[] uses;
        String[] incdir;
        @contains SrcDir[] srcdir;
        String[] compilationflags;
        String[] additionaljar;
        String language;
        boolean isAda;
        boolean isCpp;
        boolean isJava;
        boolean isRust;
        Component component; // associated component, if any
    }

    class SrcDir {
        String path;
        String[] src;
        String[] obj;
    }

    class TriggerInstance {
        String name;
        long id;
        long requestId;
    }

    class ThreadBase {
        String xmlID; // unique identifier in the form 'thread:{parent.name}/{name}'
        String name; // name of the thread (unique in the system). Max length = 30 characters (rationale: IMA channel name limits)
        long idNo; // unique identifier of the thread in the application, in [0,N-1], where N is the number of threads; same as
                  // 'globalnumber' in Mapping.xml
        String id; // identifier usable in C language, built from executable and thread names
        Executable parent;
        long stack; // stack size, in bytes, allocated to the thread.
        long relativePriority; // relative priority of the thread
        boolean isPrompt; // auto start
        boolean isExternalThread;
        InitObj[] events;
    }

    class Thread extends ThreadBase {
        long bufferSize; // size of buffer used for serialisation of operation parameters (sending events and RRs)
        long outsizemax; // size of the buffer for receiving out parameters of synchronous RR responses
        long maxBufferOutSize; // size of the buffer for sending out parameters of RR responses
        long shmoutglobalsize;
        long bufferInSize; // size of the socket buffer in recv.
        boolean publishesData; // true if it can publish at least one data
        @contains OperationGroup[] sortedOperations;
        Instance[] instances;
        String packedName; // same as 'name', but without '_'
        boolean hasShmout;
        boolean hasSupervisor;
        boolean hasAsyncTimeout;
        boolean hasAsyncRequireRequestResponses; // used to know if a task has instance that need to manage timeout.
        boolean hasSyncRequiredRequestResponses; // used to determine if a SYNC socket has to be open in the task.
        boolean hasBufferOut;
        @contains Request[] requests;
        @contains Request[] replies; // buffer for parameters of requestResponse callbacks
        long nbHandledRequests;

        boolean handleRepublishRequests; // true iff a data written by an instance on this thread is read by anyone
    }

    class Dispatch extends ThreadBase {
        String channelName;
        String destPlatformName;
        long channelPoolBlockSize;
        long channelPoolSize;
        boolean isExternal;
        @contains DispatchOperationsMapEntry[] operationsMapByExternalId;
        @contains DispatchedOperation[] sortedOperations;
        @contains DispatchedLink[] dispatchedLinks;
        @contains DispatchedLink[] requestResponseOuts;
        Channel inChannel; // if isExternal
    }

    class Request {
        long id;
        long capacity;
        long parameterSize;
    }

    class DataVersion {
        String xmlID; // unique identifier in the form 'dataversion:{parent.name}/{id}'
        DataLink dataLink;
        long id; // shortcut for dataLink.id'
        long shmglobalsize; // total shared memory size occupied by the data versions
        long numberofversions; // total number of version that must be allocated, deducted from the operation 'maxversions'
                              // attribute
        long sizeof;
        long size;
        long requestsize; // size of the request for data publication
        boolean hasDefaultValue;
        String defaultValue; // default value for the data, if any, in the syntax of the language of the dispatcher
        TypeDefinition type; // type if the data, in the language of the dispatcher
    }

    class Route {
        String xmlID; // unique identifier in the form
                      // 'route:{exec.name}/{data|event|request|response}/{operationId}[/{channelName}/{externalId}]'
        long operationId; // of operation link this route relates to
        Instance instance;
        Executable exec; // where the consumer is deployed
        String thread; // name of the thread where the consumer is deployed
        boolean isLocal; // true if the consumer is on the same platform as the producer
        boolean isActivating;
        boolean isSynchronous;
        boolean hasExternalId;
        long externalId;
        String outChannelName; // if isLocal=false, the name of the communication channel taken by the operation
        OperationLink operationLink; // if hasExternalId=true only
    }

    class TracePoint {
        long id;
        String name; // alphanum characters + '.'
        long initialState;
        boolean isBinary; // true only for observation trace points
        @contains Parameter[] parameters;
    }

    class OsProperties {
        String endianness;
        boolean isBigEndian;
        boolean supportShm;
        boolean hasLibc;
        boolean isWindows;
        boolean isListCompliant;
        boolean isStp;
        boolean supervision;
        boolean simulation;
    }

    // ---------- GENMAIN (avant refactoring) ------------

    /*
     * Les classes qui suivent maintennent la compatibilité avec les templates GenMain, mais devraient être restructurées.
     * 
     * Exec --Container ----OperationsMap ------OperationsMapEntry --------LinkedInstance ----Instance ------OperationLink
     * --Dispatch ----DispatchOperationsMapEntry ------DispatchedOperation --------OperationConsumerThread --Thread
     * ----OperationGroup
     */

    class Container {
        Component component;
        Instance[] instances;
        Executable parent;
        @contains OperationsMap operationMap;
        @contains DataConnection[] dataConnections;
        String sourceDirectory;
    }

    class OperationsMap {
        @contains OperationsMapEntry[] readData;
        @contains OperationsMapEntry[] writtenData;
        @contains OperationsMapEntry[] sentEvents;
        @contains OperationsMapEntry[] receivedEvents;
        @contains OperationsMapEntry[] providedRequestResponses;
        @contains OperationsMapEntry[] requiredRequestResponses;
    }

    class OperationsMapEntry {
        @contains LinkedInstance[] linkedInstances;
        Instance[] instances; // idem linkedInstances, mais pointe directement sur la classe Instance
        Instance[] unlinkedInstances;
        Operation operation;
        // boolean oneInSamePeriodicalTask; // logical OR of corresponding attributes on linkedInstances
        // boolean allInSamePeriodicalTask; // logical AND of corresponding attributes on linkedInstances
    }

    class LinkedInstance {
        Instance instance;
        OperationLink[] links;
        OperationLink referenceLink; // used only for data operations
        OperationLink[] otherLinks; // =links-referenceLink; used only for data operations
        TracePoint observation;
        Long[] notificationReqIds; // liste des IDs d'evenenements à lever pour notification locale
        NotificationLink[] notificationLinks;  // liste des threads à réveiller pour notification locale avec l'operation id correspondante
    }
    
    class NotificationLink {
    	Long notificationReqId;
    	Thread notifiedThread;
    }

    class OperationLink {
        String xmlID; // unique identifier in the form 'oplink:parent.name/operationName/id', or
                      // 'oplink:parent.name/operationName/id/server.name' if isRequiredRequestResponse=true
        Port port;
        long id; // of link in assembly; shortcut for data.or event.or requestResponse.id
        long callbackId; //id generated for RequestResponse callback id operation
        TypeDefinition type;
        boolean isActivating;
        boolean isNotified;
        boolean isReference;
        // Instance[] readers;
        // Instance[] writers;
        // Instance reference;
        Instance parent;
        OperationData data;
        DataLink dataLink;
        OperationEvent event;
        EventLink eventLink;
        boolean isReceivedEvent;
        boolean isDataRead;
        boolean isOnSameTask; // true if reader/writer on the same task.
        // boolean inSamePeriodicalTask; // true if parent and receivers are in the same periodical task
        Instance[] whenkos;
        @contains WhenCondition[] whenconditions;
        OperationRequestResponse requestResponse;
        RequestResponseLink requestResponseLink;
        // String requestName;
        // long requestId;
        boolean isDirect;
        OperationRequestResponse operationInServer; // defined only if isDirect=true
        long isAnswer; // warning: this is not a boolean but really an long, 0 or 1; to be renamed and changed in templates
        boolean needsDispatch; // true in case this link concerns a writer which any of the client instances is managed by another
                               // dispatcher
        boolean isProvidedRequestResponse;
        boolean isRequiredRequestResponse;
        Instance client;
        Instance server;
        OperationLink[] failoverLinks;
        ThreadBase[] destinationThreads;
    }

    class WhenCondition {
        long id; // =variable.id
        Variable variable;
        String value;
    }

    class DispatchedLink { // from Mapping.xml
        long id; // of associated link
        boolean isExternal;
        long externalId; // only used when operation is external
        long nbProducers; // from other sites than dispatching one
    }

    class DispatchOperationsMapEntry {
        long key;
        DispatchedOperation value;
    }

    class DispatchedOperation {
        String xmlID; // unique identifier in the form 'dispatchedoperation:dispatch.name/dispatch.id/reqId'
        long reqId;
        long externalId;
        long size;
        Long[] notificationReqIds; // pour une DATA reçue, liste des IDs d'evenenements à lever pour notification
                                   // locale
        boolean isReceivedEvent;
        boolean isProvidedRequestResponse;
        boolean isRequiredRequestResponse;
        boolean isReadData;
        long nbProducers;
        long sizeMax;
        @contains OperationConsumerThread[] consumers;
    }

    class OperationConsumerThread {
        OperationLink link;
        Thread thread;
        Instance[] instances; // list of instances hosted by 'thread' and involved in 'link'
        Executable executable; // shortcut for: thread.executable
        OperationLink[] links;
    }

    class OperationGroup {
        long reqId;
        OperationLink[] operations;
        boolean isReceivedEvent; // reprise de la valeur commune à toutes les opérations
        boolean isProvidedRequestResponse; // idem
        boolean isRequiredRequestResponse; // idem
        boolean isDataRead; //idem
        boolean isOnSameTask; //idem
    }

    // ---------- GENMAIN (après refactoring) ------------
    /*
     * Les classes suivantes sont un remplacement plus simple pour les classes
     * OperationsMap/OperationsMapEntry/LinkedInstance/OperationLink. Elles sont définies pour l'instant uniquement pour les
     * opérations de type Data. Travaux en cours...
     */
    class DataConnection {
        @contains LinkedInstanceData[] linkedInstances;
        OperationData operation;
    }

    class LinkedInstanceData {
        PortData port;
        DataLink referenceLink;
        DataLink[] otherLinks;
    }

    // ---------- CONFIG ------------

    class Initialization {
        @contains InitObj[] events;
        @contains InitObj[] periodicEvents;
        @contains Pool[] pools;
        @contains InstancePInfo[] pinfos;
        long globalPoolSize; // size, in bytes, of a single buffer able to store all memory pools, each one being aligned on a
                            // 8-byte boundary.
    }

    class InitObj {
        String name; // name (unique in the system). Max length = 44 characters (rationale: IMA channel name limits)
        long id;
        ThreadBase owner;
    }

    class Pool {
        String name; // name (unique in the system). Max length = 44 characters (rationale: IMA channel name limits)
        long id;
        long size; // size, in bytes, of the shared memory
    }

    class Channel {
        String xmlID; // unique identifier in the form 'channel:{executable.name}/{id}'
        String name; // name of the channel (unique in the system). Max length = 30 characters (rationale: IMA channel name
                     // limits)
        long id; // identifier in [0..N-1], where N is the number of channels in the executable
        boolean directionIn; // True if channel is a reception one, or bi-directional.
        boolean directionOut; // True if channel is an emission one, or bi-directional.
        String destSite; // name of destination platform for output data
        String address; // address of the channel
        long socket; // port numerical identifier associated to the channel
        long maxMessageSize; // size, in bytes, of the largest network message managed by the channel
        long portSize; // size, in bytes, of a port single message buffer
        long slotCount; // number of messages that can be stored in the IMA reception queue
        Dispatch dispatch; // for external/in only
        Route[] routes; // for external/out only
    }

    // class LinkedDataAccess {
    // String xmlID; // unique identifier in the form 'linkeddataaccess:TDB'
    // OperationLink link; // instanciated data link accessed from this entrypoint
    // // constraint: (Instance) link.parent == parent.parent
    // DataAccess access;
    // // constraint: (OperationEvent) access.parent == parent.event
    // }

    class ProductionInfo {
        String MWLIB_PATH;
        String[] CFLAGS;
        String[] LDFLAGS;
        String[] CFLAGS_DEBUG;
        String CC;
        String CLD;
        String CPPLD;
        String AR;
    }

}
