/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s27.ctypes;

import java.util.HashMap;
import java.util.Iterator;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.gen.common.UniqueList;
import java.io.IOException;
import com.thalesgroup.softarc.sf.Component;
import com.thalesgroup.softarc.sf.TypeDefinition;

public class CTypes extends AbstractPass {

    @Override
    public void execute() throws IOException {

        UniqueList<Component> allTypeContainersList = new UniqueList<Component>();

        allTypeContainersList.addAll(context.system.getPredefLib());
        for (Component c : context.system.getComponents()) {
            if (c.getIsLibrary())
                allTypeContainersList.add(c);
        }

        for (Component cmp : allTypeContainersList) {
            assert cmp.getCComponent() != null;
            assert cmp.getCComponent().getIsCComponent();

            computeCNames(cmp);

            linkCTypes(cmp);
        }
    }

    void computeCNames(Component cmp) {
        
        Iterator<TypeDefinition> it = cmp.getTypes().iterator();
        for (TypeDefinition cType : cmp.getCComponent().getTypes()) {
            it.next().setCName(cType.getQName());
        }
    }

    void linkCTypes(Component c) {

        if (c.getCComponent() != c) {

            HashMap<String, TypeDefinition> map = new HashMap<>();
            for (TypeDefinition td : c.getCComponent().getTypes()) {
                map.put(td.getName(), td);
            }
            for (TypeDefinition td : c.getTypes()) {
                td.setCType(map.get(td.getName()));
            }

        } else {

            for (TypeDefinition td : c.getTypes()) {
                td.setCType(td);
            }
        }
    }

}
