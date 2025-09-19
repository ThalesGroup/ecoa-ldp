/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.ecoa.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import technology.ecoa.model.datatype.CTArray;
import technology.ecoa.model.datatype.CTConstant;
import technology.ecoa.model.datatype.CTEnum;
import technology.ecoa.model.datatype.CTList;
import technology.ecoa.model.datatype.CTMap;
import technology.ecoa.model.datatype.QualifiedField;
import technology.ecoa.model.datatype.CTFixedArray;
import technology.ecoa.model.datatype.CTRecord;
import technology.ecoa.model.datatype.CTSimple;
import technology.ecoa.model.datatype.CTString;
import technology.ecoa.model.datatype.CTType;
import technology.ecoa.model.datatype.CTUnionField;
import technology.ecoa.model.datatype.CTValue;
import technology.ecoa.model.datatype.CTVariantRecord;
import technology.ecoa.model.datatype.EPredef;
import com.thalesgroup.softarc.tools.InconsistentModelError;

public abstract class TypesContainer {
    private final List<CTType> _allTypes = new ArrayList<CTType>();
    private final Map<String, CTType> _mapAllTypes = new LinkedHashMap<String, CTType>();

    private final Map<String, CTSimple> _simples = new LinkedHashMap<String, CTSimple>();
    private final Map<String, CTRecord> _records = new LinkedHashMap<String, CTRecord>();
    private final Map<String, CTEnum> _enums = new LinkedHashMap<String, CTEnum>();
    private final Map<String, CTArray> _arrays = new LinkedHashMap<String, CTArray>();
    private final Map<String, CTFixedArray> _fixedarrays = new LinkedHashMap<String, CTFixedArray>();
    private final Map<String, CTVariantRecord> _variantRecords = new LinkedHashMap<String, CTVariantRecord>();
    private final Map<String, CTString> _strings = new LinkedHashMap<String, CTString>();
    private final Map<String, CTList> _lists = new LinkedHashMap<String, CTList>();
    private final Map<String, CTMap> _maps = new LinkedHashMap<String, CTMap>();

    protected final Set<String> _usedTypes = new LinkedHashSet<String>();
    private final Set<String> _usedLibrariesNames = new LinkedHashSet<String>();
    private final Map<String, Library> _libraries = new LinkedHashMap<String, Library>();
    private String name;

    protected TypesContainer(String name, List<CTType> types) {
        this.name = name;
        List<TypeAndDependencies> localTypes = new LinkedList<TypeAndDependencies>();

        if (types != null) {
            for (CTType t : types) {
                TypeAndDependencies localType = new TypeAndDependencies(t, this);
                localTypes.add(localType);

                if (t instanceof CTSimple) {
                    localType.addDependency(((CTSimple) t).getType());
                    localType.addConstantDependency(((CTSimple) t).getMinRange());
                    localType.addConstantDependency(((CTSimple) t).getMaxRange());
                } else if (t instanceof CTEnum) {
                    localType.addDependency(((CTEnum) t).getType());
                    for (CTValue v : ((CTEnum) t).getValue()) {
                        localType.addConstantDependency(v.getValNum());
                    }
                } else if (t instanceof CTVariantRecord) {
                    CTVariantRecord type = (CTVariantRecord) t;
                    localType.addDependency(type.getSelectType());
                    for (QualifiedField f : type.getField()) {
                        localType.addDependency(f.getType());
                    }
                    for (CTUnionField f : type.getUnion()) {
                        localType.addDependency(f.getType());
                    }
                } else if (t instanceof CTRecord) {
                    for (QualifiedField f : ((CTRecord) t).getField()) {
                        localType.addDependency(f.getType());
                    }
                } else if (t instanceof CTArray) {
                    localType.addDependency(((CTArray) t).getType());
                    localType.addConstantDependency(((CTArray) t).getMaxNumber());
                } else if (t instanceof CTFixedArray) {
                    localType.addDependency(((CTFixedArray) t).getType());
                    localType.addConstantDependency(((CTFixedArray) t).getMaxNumber());
                } else if (t instanceof CTString) {
                    // No dependency to manage
                } else if (t instanceof CTList) {
                    localType.addDependency(((CTList) t).getType());
                } else if (t instanceof CTMap) {
                    localType.addDependency(((CTMap) t).getType());
                } else if (t instanceof CTConstant) {
                    localType.addDependency(((CTConstant) t).getType());
                    localType.addConstantDependency(((CTConstant) t).getValue());
                } else {
                    throw new IllegalStateException("Unexpected " + t.getClass());
                }
            }
        }

        // We have to sort the types to respect their cross-dependencies
        buildFinalTypesList(localTypes);

        // Build the list of used libraries
        registerUsedLibraries();

    }

    public void loadSubLibraries(ModelLoader loader) throws IOException {
        for (String libraryName : getUsedLibraries()) {

            Library lib = loader.loadLibrary(libraryName);
            _libraries.put(libraryName, lib);
        }
    }

    public String getName() {
        return name;
    }

    protected void buildFinalTypesList(List<TypeAndDependencies> types) {

        while (types.size() > 0) {
            // Search for any type which depends on no other
            int i = 0;
            while (i < types.size() && (types.get(i).getNbDependencies() != 0)) {
                i++;
            }

            if (i < types.size()) {
                TypeAndDependencies type = types.get(i);
                // Remove it from the list
                _allTypes.add(type.getType());
                types.remove(i);
                // Update remaining types
                for (TypeAndDependencies other : types) {
                    other.removeDependency(type.getName());
                }
            } else {
                // There is no such type; this is obviously wrong. This means
                // that there is at least one type which depends on another
                // which is not defined. 
                for (TypeAndDependencies type : types) {
                    List<String> dependencies_to_remove = new LinkedList<String>();

                    for (String dependency : type.getDependencies()) {
                        boolean found = false;
                        for (TypeAndDependencies other_type : types) {
                            if (other_type.getName().equals(dependency)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            throw new InconsistentModelError("Missing type definition for '" + dependency
                                    + "' referenced in following type(s): '" + type.getName() + "'");
                        }
                    }

                    for (String dependency : dependencies_to_remove) {
                        type.removeDependency(dependency);
                    }
                }
            }
        }

        // All types list is now sorted! Let's build sublists
        for (CTType t : _allTypes) {

            _mapAllTypes.put(t.getName(), t);

            if (t instanceof CTSimple) {
                CTSimple type = (CTSimple) t;
                _simples.put(type.getName(), type);
            } else if (t instanceof CTVariantRecord) {
                CTVariantRecord type = (CTVariantRecord) t;
                _variantRecords.put(type.getName(), type);
            } else if (t instanceof CTRecord) {
                CTRecord type = (CTRecord) t;
                _records.put(type.getName(), type);
            } else if (t instanceof CTEnum) {
                CTEnum type = (CTEnum) t;
                _enums.put(type.getName(), type);
            } else if (t instanceof CTArray) {
                CTArray type = (CTArray) t;
                _arrays.put(type.getName(), type);
            } else if (t instanceof CTFixedArray) {
                CTFixedArray type = (CTFixedArray) t;
                _fixedarrays.put(type.getName(), type);
            } else if (t instanceof CTString) {
                CTString type = (CTString) t;
                _strings.put(type.getName(), type);
            } else if (t instanceof CTList) {
                CTList type = (CTList) t;
                _lists.put(type.getName(), type);
            } else if (t instanceof CTMap) {
                CTMap type = (CTMap) t;
                _maps.put(type.getName(), type);
            }
        }
    }

    protected void registerUsedLibraries() {
        for (String typename : _usedTypes) {
            int i = typename.indexOf('.');
            if (i >= 0) {
                String libraryName = typename.substring(0, i);
                // warning: for a Component using a Library, libraryName may be equal to the name of the using component.
                if (!libraryName.equals(this.name) || this instanceof ComponentType)
                    _usedLibrariesNames.add(libraryName);
            }
        }
    }

    /**
     * @return The whole list of types declared by this component/library. Note that types are sorted according to their
     *         cross-dependencies.
     */
    public List<CTType> getAllTypes() {
        return _allTypes;
    }

    /**
     * @return The list of types that this component/library depends on
     */
    public Set<String> getUsedTypes() {
        return _usedTypes;
    }

    /**
     * @return The list of libraries that this component/library depends on
     */
    public Set<String> getUsedLibraries() {
        return _usedLibrariesNames;
    }

    public Map<String, CTSimple> getSimples() {
        return _simples;
    }// Map< String, CTSimple > getSimples()

    public CTSimple getSimple(String name) {
        return _simples.get(name);
    }// CTSimple getSimple( String name )

    public Map<String, CTRecord> getRecords() {
        return _records;
    }// Map< String, CTRecord > getRecords()

    public CTRecord getRecord(String name) {
        return _records.get(name);
    }// CTRecord getRecord( String name )

    public Map<String, CTEnum> getEnums() {
        return _enums;
    }// Map< String, CTEnum > getEnums()

    public CTEnum getEnum(String name) {
        return _enums.get(name);
    }// CTEnum getEnum( String name )

    public Map<String, CTArray> getArrays() {
        return _arrays;
    }// Map< String, CTArray > getArrays()

    public CTArray getArray(String name) {
        return _arrays.get(name);
    }// CTArray getArray( String name )

    public Map<String, CTString> getStrings() {
        return _strings;
    }// Map< String, CTString > getStrings()

    public CTString getString(String name) {
        return _strings.get(name);
    }// CTString getString( String name )

    public Map<String, CTFixedArray> getFixedArrays() {
        return _fixedarrays;
    }// Map< String, CTFixedArray > getFixedArrays()

    public CTFixedArray getFixedArray(String name) {
        return _fixedarrays.get(name);
    }// CTFixedArray getFixedArray( String name )

    public Map<String, CTVariantRecord> getVariantRecords() {
        return _variantRecords;
    }// Map< String, CTVariantRecord > getVariantRecords()

    public CTVariantRecord getVariantRecord(String name) {
        return _variantRecords.get(name);
    }// CTVariantRecord getVariantRecord( String name )

    public Map<String, CTList> getLists() {
        return _lists;
    }// Map< String, CTList > getLists()

    public CTList getList(String name) {
        return _lists.get(name);
    }// CTList getList( String name )

    public Map<String, CTMap> getMaps() {
        return _maps;
    }// Map< String, CTMap > getMaps()

    public CTMap getMap(String name) {
        return _maps.get(name);
    }// CTMap getMap( String name )

    /**
     * Inner class which gathers a type and the list of typenames it depends on
     */
    private class TypeAndDependencies {

        private final CTType _type;
        private final TypesContainer _parent;
        private final Set<String> dependencies = new LinkedHashSet<String>();

        public TypeAndDependencies(CTType type, TypesContainer parent) {
            _type = type;
            _parent = parent;
        }

        public String getName() {
            return _type.getName();
        }

        public CTType getType() {
            return _type;
        }

        public void addDependency(String typename) {
            int dotIndex = typename.indexOf('.');
            // If the typename is not prefixed, and is not a predefined
            // type : it is a local type
            if (dotIndex == -1) {
                try {
                    EPredef.fromValue(typename);
                } catch (IllegalArgumentException e) {
                    dependencies.add(typename);
                }
            } else {
                String parent = typename.substring(0, dotIndex);
                String type = typename.substring(dotIndex + 1);
                // If type owner is us, this is a local type!
                if (parent.compareTo(_parent.getName()) == 0) {
                    dependencies.add(type);
                }
            }
            // In any case, the type is used!
            _usedTypes.add(typename);
        }

        public void addConstantDependency(String value) {
            if (value != null && value.startsWith("%")) {
                addDependency(value.replaceAll("%", ""));
            }
        }

        public void removeDependency(String typename) {
            dependencies.remove(typename);
        }

        public int getNbDependencies() {
            return dependencies.size();
        }

        public Set<String> getDependencies() {
            return dependencies;
        }
    }

    public Library getLibrary(String libraryName) {
        return _libraries.get(libraryName);
    }

    public CTType getType(String typeName) {
        return _mapAllTypes.get(typeName);
    }
    
    public Set<TypesContainer> computeAllUsedLibraries() {
        Set<TypesContainer> dependencies = new LinkedHashSet<TypesContainer>();
        for (String usedlibName : getUsedLibraries()) {
            Library lib = getLibrary(usedlibName);
            if (lib != null) {
                if (dependencies.add(lib) == true) {
                    dependencies.addAll(lib.computeAllUsedLibraries());
                }
            }
        }
        return dependencies;
    }
}
