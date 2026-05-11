/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.technicalassembly;

public class Wire {

    public Operation source;
    public Operation target;
    public boolean activating = true;
    public boolean callbackActivating;
    public boolean direct;
    public EDR kind;
    public WhenSet sourceWhen;
    public WhenSet targetWhen;
    public long sourceFifoSize = 0;
    public long targetFifoSize = 0;
    public String inChannel;
    public String outChannel;
    public Long originLinkId;

    public Wire(Operation source, Operation target, EDR kind) {
        super();
        assert source != null;
        assert target != null;
        this.source = source;
        this.target = target;
        this.kind = kind;
        sourceWhen = new WhenSet();
        targetWhen = new WhenSet();
    }

    public Wire(Wire first, Wire second) {
        super();
        assert first.target == second.source;
        this.source = first.source;
        this.target = second.target;
        assert first.kind == second.kind;
        this.kind = first.kind;
        this.activating = first.activating || second.activating;
        this.callbackActivating = first.callbackActivating || second.callbackActivating;
        this.direct = first.direct && second.direct;
        sourceWhen = new WhenSet(first.sourceWhen);
        sourceWhen.addAll(second.sourceWhen);
        targetWhen = new WhenSet(first.targetWhen);
        targetWhen.addAll(second.targetWhen);
        this.sourceFifoSize = Math.max(first.sourceFifoSize, second.sourceFifoSize);
        this.targetFifoSize = Math.max(first.targetFifoSize, second.targetFifoSize);
        this.originLinkId = second.originLinkId;
    }

    public Wire(Wire old) {
        super();
        this.source = old.source;
        this.target = old.target;
        this.kind = old.kind;
        this.activating = old.activating;
        this.callbackActivating = old.callbackActivating;
        this.direct = old.direct;
        sourceWhen = new WhenSet(old.sourceWhen);
        targetWhen = new WhenSet(old.targetWhen);
        this.sourceFifoSize = old.sourceFifoSize;
        this.targetFifoSize = old.targetFifoSize;
        this.originLinkId = old.originLinkId;
    }

    @Override
    public String toString() {
        return toSourceString() + "->" + target.toString();
    }

    public String toSourceString() {
        StringBuilder sb = new StringBuilder(source.toString());
        for (Condition i : sourceWhen.whencondition) {
            sb.append("/when:");
            sb.append(i.text);
        }
        if (!activating) {
            sb.append("/nonactivating");
        }
        return sb.toString();
    }

    public boolean isReflexive() {
        return source == target;
    }

}
