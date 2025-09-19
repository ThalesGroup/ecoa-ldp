/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s43.checksignatures;

import java.util.ArrayList;
import java.util.Collection;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import java.io.IOException;
import com.thalesgroup.softarc.sf.Assembly;
import com.thalesgroup.softarc.sf.DataLink;
import com.thalesgroup.softarc.sf.DataLinkElement;
import com.thalesgroup.softarc.sf.EnumValue;
import com.thalesgroup.softarc.sf.EventLink;
import com.thalesgroup.softarc.sf.EventLinkReceiver;
import com.thalesgroup.softarc.sf.EventLinkSender;
import com.thalesgroup.softarc.sf.Link;
import com.thalesgroup.softarc.sf.Operation;
import com.thalesgroup.softarc.sf.OperationData;
import com.thalesgroup.softarc.sf.OperationEvent;
import com.thalesgroup.softarc.sf.OperationRequestResponse;
import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.Port;
import com.thalesgroup.softarc.sf.RequestResponseLink;
import com.thalesgroup.softarc.sf.RequestResponseLinkReceiver;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.VariantField;

public class CheckSignatures extends AbstractPass {

    private ArrayList<String> problems = new ArrayList<>();

    @Override
    public void execute() throws IOException {

        if (context.featureToggles.contains("nochecks"))
            return;

        problems.clear();
        Assembly assembly = context.system.getAssembly();

        for (EventLink evLink : assembly.getEventLinks()) {
            SignatureChecker sig = new SignatureChecker(evLink);
            for (EventLinkSender sender : evLink.getSenders()) {
                sig.check(sender.getPort());
            }
            for (EventLinkReceiver receiver : evLink.getReceivers()) {
                sig.check(receiver.getPort());
            }
        }

        for (RequestResponseLink sLink : assembly.getRequestResponseLinks()) {
            SignatureChecker sig = new SignatureChecker(sLink);
            for (RequestResponseLinkReceiver sp : sLink.getServers()) {
                sig.check(sp.getPort());
            }
            if (sLink.getClient() != null) {
                sig.check(sLink.getClient().getPort());
            }
        }

        for (DataLink dataLink : assembly.getDataLinks()) {
            SignatureChecker sig = new SignatureChecker(dataLink);
            for (DataLinkElement writer : dataLink.getWriters()) {
                sig.check(writer.getPort());
            }
            for (DataLinkElement reader : dataLink.getReaders()) {
                sig.check(reader.getPort());
            }
        }

        if (!problems.isEmpty()) {
            for (String s : problems) {
                warning("%s", s);
            }
            errorModel("Incompatible signatures between operations linked together in assembly: see details above");
        }
    }

    private class SignatureChecker {
        String lexicalSignature = null;
        Port port1;
        Link link;

        public SignatureChecker(Link link) {
            this.link = link;
        }

        void check(Port port) {
            Operation op = port.getOperation();
            if (lexicalSignature == null) {
                lexicalSignature = calculateLexical(op);
                port1 = port;
            } else {
                String other = calculateLexical(op);
                if (!lexicalSignature.equals(other)) {
                    problems.add("In " + link.getId() + ", found incompatible signatures for operations:");
                    problems.add(port.getXmlID() + " has: ");
                    problems.add(other);
                    problems.add(port1.getXmlID() + " has: ");
                    problems.add(lexicalSignature);
                }
            }
        }
    }

    private String calculateLexical(Operation op) {
        List res = null;
        if (op instanceof OperationData) {
            res = new List("data");
            res.add(getLexicalTypeValue(((OperationData) op).getType()));
        } else if (op instanceof OperationEvent) {
            res = new List("event");
            res.addAll(op.getInParameters());
        } else if (op instanceof OperationRequestResponse) {
            res = new List("service");
            res.addAll(op.getInParameters());
            res.add("->");
            res.addAll(op.getOutParameters());
        }
        assert res != null;
        return res.toString();
    }

    private String getLexicalTypeValue(TypeDefinition type) {
        if (type.getIsPredef())
            return type.getName();

        if (type.getIsSimple())
            return getLexicalTypeValue(type.getBaseType());

        List res = null;
        if (type.getIsEnum()) {
            // For an enumerated Type, the lexical signature is composed of the base type
            // plus the ordered set of authorized values
            res = new List(getLexicalTypeValue(type.getBaseType()));
            for (EnumValue tag : type.getEnumValues()) {
                res.add(Long.toString(tag.getValnum()));
            }
        }

        else if (type.getIsRecord()) {
            res = new List("record");
            for (Parameter field : type.getFields()) {
                res.add(getLexicalTypeValue(field.getType()));
            }
        } else if (type.getIsVariantRecord()) {
            res = new List("variant");
            res.add(getLexicalTypeValue(type.getBaseType()));
            for (Parameter field : type.getFields()) {
                res.add(getLexicalTypeValue(field.getType()));
            }
            for (VariantField union : type.getUnionFields()) {
                res.add(getLexicalTypeValue(union.getType()));
            }
        } else if (type.getIsArray()) {
            res = new List("array", type.getArraySize());
            res.add(getLexicalTypeValue(type.getBaseType()));
        } else if (type.getIsFixedArray()) {
            res = new List("fixedarray", type.getArraySize());
            res.add(getLexicalTypeValue(type.getBaseType()));
        } else if (type.getIsString()) {
            res = new List("string", type.getLength());
        } else if (type.getIsList()) {
            res = new List("list", type.getArraySize());
            res.add(getLexicalTypeValue(type.getBaseType()));
        } else if (type.getIsMap()) {
            res = new List("map", type.getArraySize());
            res.add(getLexicalTypeValue(type.getBaseType()));
            res.add(getLexicalTypeValue(type.getKeyType()));
        }

        if (res == null)
            return "??";

        return res.toString();
    }

    class List {
        StringBuilder sb;
        int n = 0;

        List(String name) {
            sb = new StringBuilder(name);
        }

        List(String name, long arg1) {
            sb = new StringBuilder(name);
            add(Long.toString(arg1));
        }

        void add(String arg) {
            if (arg != null) {
                if (n == 0)
                    sb.append('(');
                else
                    sb.append(',');
                n++;
                sb.append(arg);
            }
        }

        void addAll(Collection<Parameter> parameter) {
            for (Parameter p : parameter) {
                add(getLexicalTypeValue(p.getType()));
            }
        }

        @Override
        public String toString() {
            if (n > 0)
                sb.append(')');
            return sb.toString();
        }

    }
}