/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.thalesgroup.ecoa.model.Assembly;
import com.thalesgroup.ecoa.model.Instance;
import technology.ecoa.model.componenttype.CTData;
import technology.ecoa.model.componenttype.CTRequestReceived;
import technology.ecoa.model.componenttype.CTReadData;
import technology.ecoa.model.componenttype.CTReceivedEvent;
import technology.ecoa.model.componenttype.CTRequestSent;
import technology.ecoa.model.componenttype.CTSentEvent;
import technology.ecoa.model.componenttype.CTWrittenData;
import technology.ecoa.model.assembly.ASDataLink;
import technology.ecoa.model.assembly.ASEventLink;
import technology.ecoa.model.assembly.ASOpRef;
import technology.ecoa.model.assembly.ASOpRefClient;
import technology.ecoa.model.assembly.ASOpRefReceive;
import technology.ecoa.model.assembly.ASOpRefServer;
import technology.ecoa.model.assembly.ASOpRefWrite;
import technology.ecoa.model.assembly.ASOperationLink;
import technology.ecoa.model.assembly.ASRequestResponseLink;
import technology.ecoa.model.assembly.ASWhenCondition;

import com.thalesgroup.softarc.tools.InconsistentModelError;

/**
 * A composite is a component type which contains other component types.
 * 
 * It is defined by an internal assembly.
 */
public class CompositeInstance extends InstanceAdapter {
    /** The instances defined inside this instance, excluding special instance "extern" */
    protected final List<InstanceAdapter> _instances;

    protected List<Wire> wires;

    /**
     * The internal assembly model that defines the type of the composite (can be shared between all instances of the same
     * component type)
     */
    public final Assembly _internalAssembly;

    /** The original internal assembly file, for self-inclusion checks */
    final File _assemblyFile;

    /** The variables that are exported ouside the composite (key=name --> value=(virtualname)) */
    final Map<String, VariableReference> _variableAliases = new LinkedHashMap<String, VariableReference>();

    protected final GenTechnicalAssembly _gen;

    /**
     * Creation of a composite.
     * 
     * @param instance The original instance, from an assembly model (null for root instance).
     * @param parent The instance in which this one is included (null for root instance).
     * @param internalAssembly The internal assembly of the composite.
     * @param loader A class that can load other models (CT and CI).
     * @param assemblyFile The original assembly file
     * @throws IOException
     */
    public CompositeInstance(Instance instance, CompositeInstance parent, Assembly internalAssembly, GenTechnicalAssembly gen,
            File assemblyFile) throws Exception {
        super(instance, parent);

        if (instance == null && internalAssembly.getToplevelComponentType() != null) {
            setComponentType(internalAssembly.getToplevelComponentType());
        }
        _internalAssembly = internalAssembly;
        _instances = new ArrayList<InstanceAdapter>();
        wires = new ArrayList<Wire>();
        _assemblyFile = assemblyFile;
        _gen = gen;

        checkNonEmpty();
        checkSelfInclusion(parent);

        buildInstances();
        
        ImplicitLinks il = new ImplicitLinks(this);
        il.createNewLinks();

        buildDataLinks();
        buildEventLinks();
        buildRequestResponseLinks();
    }
    
    /**
     * Construction of the component instances from the original assembly.
     * 
     * @throws IOException
     */
    private void buildInstances() throws Exception {
        for (String iName : _internalAssembly.instances.keySet()) {
            Instance i = _internalAssembly.instances.get(iName);

            if (i.getImplementation() != null && i.getImplementation().isComposite()) {
                File internalAssemblyFile = _gen.workspace.getInternalAssembly(i.getInstance().getComponentType(),
                        i.getInstance().getImplementation());
                Assembly assemblyModel = _gen.modelLoader.loadAssemblyDeeply(internalAssemblyFile);
                CompositeInstance compositeInstance = new CompositeInstance(i, this, assemblyModel, _gen, internalAssemblyFile);
                _instances.add(compositeInstance);
            } else {
                // "extern" pseudo instance is not added to instance list
                if (!i.isExtern()) {
                    _instances.add(new InstanceAdapter(i, this));
                }
            }
        }
    }

    private void checkNonEmpty() {
        if (_internalAssembly.instances.isEmpty())
            throw new InconsistentModelError(String.format(
                    "Instance %s: empty composite implementations are forbidden; for a stub, create an implementation in C without any manual code inside",
                    fullName));
    }

    /**
     * Check that this composite does not have the same definition as a parent instance (else, infinite recursion).
     * 
     * @param ancestor another composite instance that includes this one.
     */
    private void checkSelfInclusion(CompositeInstance ancestor) {
        if (ancestor != null) {
            if (_assemblyFile.equals(ancestor._assemblyFile))
                throw new InconsistentModelError(
                        String.format("Instance %s is implemented by the same composite as its parent instance %s", fullName,
                                ancestor.fullName));
            checkSelfInclusion(ancestor._parent);
        }
    }

    /**
     * Get the instance associated to the given instancename.
     * 
     * @param instancename An instance name.
     * @return The instance associated to the given instancename.
     */
    public InstanceAdapter getInstance(String instancename) {
        for (InstanceAdapter instanceAdapter : _instances)
            if (instanceAdapter.getShortName().equals(instancename))
                return instanceAdapter;
        throw new InconsistentModelError(String.format("Instance %s not found", instancename));
    }

    public ComponentInstance getInstanceByFullname(String instancename) {
        ComponentInstance result = null;

        for (ComponentInstance instanceAdapter : _instances)
            if (instanceAdapter.getFullName().equals(instancename))
                result = instanceAdapter;

        return result;
    }

    private int computeFifoSize(ASOperationLink link, ASOpRef e) {
        long fifoSizeDefault = 1;
        Long fifoSize = null;
        if (e instanceof ASOpRefReceive) {
            fifoSize = ((ASOpRefReceive) e).getFifoSize();
        } else if (e instanceof ASOpRefClient) {
            fifoSize = ((ASOpRefClient) e).getFifoSize();
        } else if (e instanceof ASOpRefServer) {
            fifoSize = ((ASOpRefServer) e).getFifoSize();
        }
        return (fifoSize != null ? fifoSize.intValue() : (int) fifoSizeDefault);
    }

    private boolean computeActivating(ASOperationLink link, ASOpRef e) {
        boolean isActivatingDefault = true;
        Boolean isActivating = null;
        if (e instanceof ASOpRefReceive) {
            isActivating = ((ASOpRefReceive) e).isActivating();
        } else if (e instanceof ASOpRefClient) {
            isActivating = ((ASOpRefClient) e).isCallbackActivating();
        } else if (e instanceof ASOpRefServer) {
            isActivating = ((ASOpRefServer) e).isActivating();
        }
        return (isActivating != null ? isActivating.booleanValue() : isActivatingDefault);
    }

    /**
     * Construction of the data links from the original assembly.
     */
    private void buildDataLinks() {
        ArrayList<ASOpRef> consumers = new ArrayList<ASOpRef>();
        for (ASDataLink datalink : _internalAssembly.dataLinks) {
            // build list of all consumers for this datalink
            // first, pure readers
            consumers.clear();
            consumers.addAll(datalink.getReader());
            // then, add writers which are also readers (i.e, if reference="true", and not writeOnly, and not external)
            for (ASOpRefWrite consumer : datalink.getWriter()) {
                if (consumer.isReference()) {
                    Operation op = resolveOperation(consumer);
                    // do not add writers tagged 'writeonly', which are not really consumers
                    // (except for external operations which are seen reversed)
                    if (!op.writeOnly && !op.isExternalOperationOf(this))
                        consumers.add(consumer);
                }
            }

            // now, look for all producers
            for (ASOpRefWrite producer : datalink.getWriter()) {
                Operation writerOp = resolveOperation(producer);

                // Check that a <writer> element is connected to a <data_written> operation.
                // For composites, the operation (seen from the outside) can be data_read or data_written.
                checkTypeOfOperation(writerOp, "a <writer> in a <datalink>", CTWrittenData.class, CTData.class);

                // for this producer, loop on consumers
                for (ASOpRef consumer : consumers) {
                    Operation readerOp = resolveOperation(consumer);
                    // is this consumer also a producer?
                    boolean isWrite = consumer instanceof ASOpRefWrite;
                    if (!isWrite)
                        // Check that a <reader> element is connected to a <data_read> operation.
                        // For composites, the operation (seen from the outside) must be a data_written operation.
                        checkTypeOfOperation(readerOp, "a <reader> in a <datalink>", CTReadData.class, CTWrittenData.class);
                    Wire w = new Wire(writerOp, readerOp, Wire.Kind.DATA);
                    addSourceConditions(producer, w);
                    if (!isWrite) {
                        addTargetConditions(consumer, w);
                    }
                    w.direct = datalink.isUncontrolledAccess();
                    wires.add(w);
                }
            }
            // set default values on reader operations (even if no writer exist)
            if (datalink.getDefaultValue() != null) {
                for (ASOpRef consumer : consumers) {
                    Operation readerOp = resolveOperation(consumer);
                    readerOp.defaultvalue = datalink.getDefaultValue();
                }
            }
        }
    }

    void addSourceConditions(ASOpRef e, Wire w) {
        for (ASWhenCondition whenc : e.getWhen()) {
            w.sourceWhen.whencondition.add(new Condition(whenc, getInstance(whenc.getInstance())));
        }
    }

    void addTargetConditions(ASOpRef e, Wire w) {
        for (ASWhenCondition whenc : e.getWhen()) {
            w.targetWhen.whencondition.add(new Condition(whenc, getInstance(whenc.getInstance())));
        }
    }

    /**
     * Construction of the service links from the original assembly.
     */
    private void buildRequestResponseLinks() {
        for (ASRequestResponseLink link : _internalAssembly.serviceLinks) {
            Operation source = resolveOperation(link.getClient());
            checkTypeOfOperation(source, "a <client> in a <servicelink>", CTRequestSent.class, CTRequestReceived.class);
            for (ASOpRef server : link.getServer()) {
                Operation dest = resolveOperation(server);
                checkTypeOfOperation(dest, "a <server> in a <servicelink>", CTRequestReceived.class, CTRequestSent.class);
                Wire w = new Wire(source, dest, Wire.Kind.SERVICE);
                w.sourceFifoSize = computeFifoSize(link, link.getClient());
                w.targetFifoSize = computeFifoSize(link, server);
                // REQ
                w.activating = computeActivating(link, server);
                w.callbackActivating = computeActivating(link, link.getClient());
                wires.add(w);
                addTargetConditions(server, w);
            }
        }
    }

    /**
     * Construction of the event links from the original assembly.
     */
    private void buildEventLinks() {
        for (ASEventLink link : _internalAssembly.eventLinks) {
            for (ASOpRef producer : link.getSender()) {
                for (ASOpRef consumer : link.getReceiver()) {
                    Operation dest = resolveOperation(consumer);
                    checkTypeOfOperation(dest, "a <receiver> in an <eventlink>", CTReceivedEvent.class, CTSentEvent.class);
                    Operation source = resolveOperation(producer);
                    checkTypeOfOperation(source, "a <sender> in an <eventlink>", CTSentEvent.class, CTReceivedEvent.class);
                    Wire w = new Wire(source, dest, Wire.Kind.EVENT);
                    w.targetFifoSize += computeFifoSize(link, consumer);
                    // REQ
                    w.activating = computeActivating(link, consumer);
                    wires.add(w);
                    addSourceConditions(producer, w);
                    addTargetConditions(consumer, w);
                }
            }
        }
    }

    private String getOperationType(Class<?> cl) {
        if (cl == CTReadData.class)
            return "<data_read>";
        else if (cl == CTWrittenData.class)
            return "<data_written>";
        else if (cl == CTData.class)
            return "<data_read> or <data_written>";
        else if (cl == CTSentEvent.class)
            return "<event_sent>";
        else if (cl == CTReceivedEvent.class)
            return "<event_received>";
        else if (cl == CTRequestSent.class)
            return "<service_required>";
        else if (cl == CTRequestReceived.class)
            return "<service_provided>";
        return null;
    }

    private void checkTypeOfOperation(Operation op, String roleInAssembly, Class<?> expectedType, Class<?> expectedTypeComposite) {
        String complement = "";
        if (op.isExternalOperationOf(this)) {
            // reverse the type (because it is used from the inside of the composite)
            expectedType = expectedTypeComposite;
            complement = String.format(" (seen as a %s from inside the composite)", getOperationType(expectedTypeComposite));
        }
        if (!expectedType.isInstance(op.origin)) {
            String context = isRoot() ? " of the assembly" : " of the internal assembly of composite instance " + this.fullName;
            throw new InconsistentModelError(String.format("Operation '%s.%s' is a %s%s; it can't be used as %s%s", op.instance.componentType.getName(), op.name,
                    getOperationType(op.origin.getClass()), complement, roleInAssembly, context));
        }
    }

    Operation resolveOperation(ASOpRef e) {
        ComponentInstance i;
        if (e.getInstance().equals(Instance.SPECIAL_INSTANCE_NAME_EXTERN)) {
            if (this.componentType == null)
                _gen.errorModel(
                        "In assembly %s, the instance '%s' is used, so the root element <assembly> shall define an attribute 'componentType'",
                        _assemblyFile.getPath(), Instance.SPECIAL_INSTANCE_NAME_EXTERN);
            i = this;
        } else {
            i = getInstance(e.getInstance());
        }
        Operation operation = i.operations.get(e.getOperation());
        if (operation == null) {
            _gen.errorModel("In assembly %s, instance '%s' of type '%s' does not define any operation '%s'",
                    _assemblyFile.getPath(), e.getInstance(), i.componentType.getName(), e.getOperation());
        }
        return operation;
    }

    @Override
    public boolean isComposite() {
        return true;
    }

    public void resolveAttributes() {
        super.resolveAttributes();
        for (InstanceAdapter i : _instances) {
            i.resolveAttributes();
        }
    }

    /**
     * Transform this composite so that all real (i.e. non-composite) sub-instances (direct and indirect) are become directly
     * included instances. All the internal links are also moved up to this level.
     * 
     * The links that cross different levels (i.e. links that involve "extern" or composite instance) are merged.
     * 
     * After the transformation:
     * <li>the composite instances can be ignored because they are useless.
     * <li>the link elements that
     * involve "extern" or composite instance can also be ignored.
     * 
     */
    void flattenInstances() {

        LinkedHashSet<CompositeInstance> compositeInstances = new LinkedHashSet<CompositeInstance>();
        for (ComponentInstance instance : _instances)
            if (instance instanceof CompositeInstance)
                compositeInstances.add((CompositeInstance) instance);

        for (CompositeInstance ci : compositeInstances) {
            _gen.info("**** Processing composite instance %s ****", ci.fullName);

            // first, flatten the sub-instance (recursion)
            ci.flattenInstances();

            // REQ: for an internalWire and an externalWire, if internalWire goes to the
            LinkedList<Wire> externalWires = new LinkedList<Wire>();
            for (Wire w : wires) {
                if (w.source.instance == ci || w.target.instance == ci)
                    externalWires.add(w);
            }
            LinkedList<Wire> candidates = new LinkedList<Wire>(ci.wires);
            while (!candidates.isEmpty()) {
                Wire internalWire = candidates.pop();
                if (internalWire.source.instance == ci) {
                    for (Wire externalWire : externalWires) {
                        tryToMerge(externalWire, internalWire, candidates);
                    }
                } else if (internalWire.target.instance == ci) {
                    for (Wire externalWire : externalWires) {
                        tryToMerge(internalWire, externalWire, candidates);
                    }
                } else {
                    // REQ: copy purely internal wires to the upper level
                    wires.add(internalWire);
                }
            }
            // now that the sub-instance ci has no more links, move its instances to the upper
            // level:
            _instances.addAll(ci._instances);
            // now ci is empty: it can safely be removed
            _instances.remove(ci);
        }

        ArrayList<Wire> newList = new ArrayList<Wire>();
        for (Wire w : wires) {
            if (!(compositeInstances.contains(w.source.instance) || compositeInstances.contains(w.target.instance)))
                newList.add(w);
        }
        wires = newList;
    }

    private void tryToMerge(Wire w1, Wire w2, LinkedList<Wire> candidates) {
        if (w1.target == w2.source && !w1.isReflexive() && !w2.isReflexive()) {
            _gen.info("new candidate %s %s", w1, w2);
            candidates.push(new Wire(w1, w2));
        }
    }

    /**
     * For every element <whencondition instance="I" variable="V" value="X"/>, where I is a composite instance, then: I must be
     * replaced by an subinstance of I, V must be replaced by the aliased variable name.
     */
    public void resolveVariableAliases() {
        for (InstanceAdapter i : _instances) {
            i.resolveVariableAliases();
        }

        for (Wire w : wires) {
            ArrayList<Condition> list = new ArrayList<Condition>();
            list.addAll(w.sourceWhen.whencondition);
            list.addAll(w.targetWhen.whencondition);
            for (Condition whenc : list) {
                VariableReference vRef = whenc.variable;
                if (vRef.instance instanceof CompositeInstance) {
                    // found a whencondition referring to a composite:
                    // change the variable reference to another one.
                    VariableReference realVariable = ((CompositeInstance) vRef.instance)._variableAliases.get(vRef.variableName);
                    if (realVariable == null)
                        throw new InconsistentModelError(String.format("Variable %s is not defined on composite instance %s",
                                vRef.variableName, vRef.instance.getFullName()));
                    _gen.info("replacing variable %s by %s", vRef.variableName, realVariable);
                    whenc.variable = realVariable;
                }
            }
        }
    }

    public void dump() {
        _gen.info("==================================");
        for (Wire w : wires) {
            String sig = w.toString();
            _gen.info(sig);
        }
        _gen.info("----------------------------------");
    }

    public Assembly getInternalAssembly() {
        return _internalAssembly;
    }

}
