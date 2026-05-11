/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

import com.thalesgroup.softarc.sf.Parameter;
import com.thalesgroup.softarc.sf.TypeDefinition;
import com.thalesgroup.softarc.sf.VariantField;

// Détermine les caractéristiques d'un type (alignement, taille, etc.)

public class TypeSizer {

    // Dimensionne le type de nom 'typeName' à partir de sa définition dans le
    // 'model' correspondant. Aucune mémoire cache n'est en place, ce qui est
    // potentiellement dommageable sur de gros projets.

    public static void computeTypeSize(TypeDefinition type, TypeSizeContext result) {

        if (type.getIsSimple() || type.getIsEnum()) {
            computeTypeSize(type.getBaseType(), result);
        } else if (type.getIsPredef()) {
            computePredefTypeSize(type.getName(), result);
        } else if (type.getIsArray()) {
            computeArraySize(type, result);
        } else if (type.getIsFixedArray()) {
            computeFixedArraySize(type, result);
        } else if (type.getIsList()) {
            computeListSize(type, result);
        } else if (type.getIsMap()) {
            computeMapSize(type, result);
        } else if (type.getIsRecord()) {
            computeRecordSize(type, result);
        } else if (type.getIsVariantRecord()) {
            computeVariantRecordSize(type, result);
        } else if (type.getIsString()) {
            computeStringSize(type, result);
        }

        type.setAlignment(result.alignment);
        type.setSize(result.rawSize);
        type.setSizeof(result.sizeof);
    }

    public static long alignedSize(long size, long alignment) {
        return alignment * ((size + alignment - 1) / alignment);
    }

    public static long alignedNonZeroSize(long size, long alignment) {
        if (size == 0)
            size = 8;
        return alignedSize(size, alignment);
    }

    // Dimensionne un tableau de taille fixe

    private static void computeFixedArraySize(TypeDefinition type, TypeSizeContext result) {
        long maxNumber = type.getArraySize();
        TypeSizeContext innerTypeSizeCtxt = new TypeSizeContext();

        // array elements
        computeTypeSize(type.getBaseType(), innerTypeSizeCtxt);
        result.addArray(maxNumber, innerTypeSizeCtxt);
    }

    // Dimensionne un tableau de taille variable

    private static void computeArraySize(TypeDefinition type, TypeSizeContext result) {
        long maxNumber = type.getArraySize();

        TypeSizeContext count_size = new TypeSizeContext();
        TypeSizeContext element_size = new TypeSizeContext();

        // array header : number of elements
        computePredefTypeSize("int32", count_size);
        result.addField(count_size);

        // array elements
        computeTypeSize(type.getBaseType(), element_size);
        result.addArray(maxNumber, element_size);

        result.finalize();
    }

    // Dimensionne une liste

    private static void computeListSize(TypeDefinition type, TypeSizeContext result) {
        TypeSizeContext list = new TypeSizeContext();
        TypeSizeContext uint32 = new TypeSizeContext();
        TypeSizeContext item = new TypeSizeContext();
        long capacity = type.getArraySize();
        long raw_size = result.rawSize;

        computePredefTypeSize("uint32", uint32);

        // First, compute list size as a uint64 C array
        list.alignment = 8;
        list.addArray(5 + capacity, uint32);

        computeTypeSize(type.getBaseType(), item);
        list.addArray(capacity, item);

        list.finalize();

        // Then, insert such array in more global type
        result.addField(list);
        result.finalize();

        // The index is not serialized
        result.rawSize = raw_size + 4 + item.rawSize * capacity;
    }

    // Dimensionne une map

    private static void computeMapSize(TypeDefinition type, TypeSizeContext result) {
        TypeSizeContext map = new TypeSizeContext();
        TypeSizeContext uint32 = new TypeSizeContext();
        TypeSizeContext uint64 = new TypeSizeContext();
        TypeSizeContext key = new TypeSizeContext();
        TypeSizeContext value = new TypeSizeContext();
        TypeSizeContext item = new TypeSizeContext();
        long capacity = type.getArraySize();
        long raw_size = result.rawSize;

        computePredefTypeSize("uint32", uint32);
        computePredefTypeSize("uint64", uint64);
        computeTypeSize(type.getKeyType(), key);
        computeTypeSize(type.getBaseType(), value);

        // Map type is a difficult one: in memory, it shall be seen as an
        // array of uint64, but its serialized size depends on the size of the
        // keys

        // A map is implemented as a list of (uint64, value) items
        item.addField(uint64);
        item.addField(value);
        item.finalize();

        map.alignment = 8;
        map.addArray(7 + capacity, uint32);
        map.addArray(capacity, item);
        map.finalize();

        // Insert map inside more global type
        result.addField(map);
        result.finalize();

        // The index is not serialized, and the size used by keys is specific
        result.rawSize = raw_size + uint32.rawSize + capacity * (key.rawSize + value.rawSize);
    }

    // Dimensionne une structure (sans discriminant)

    private static void computeRecordSize(TypeDefinition type, TypeSizeContext result) {
        // each field is append sequentially
        for (Parameter field : type.getFields()) {
            TypeSizeContext fieldTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(field.getType(), fieldTypeSizeCtxt);
            result.addField(fieldTypeSizeCtxt);
        }

        result.finalize();
    }

    // Dimensionne une structure à discriminant

    private static void computeVariantRecordSize(TypeDefinition type, TypeSizeContext result) {
        // discriminant
        {
            TypeSizeContext selectTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(type.getBaseType(), selectTypeSizeCtxt);
            result.addField(selectTypeSizeCtxt);
        }

        // champs fixes
        for (Parameter field : type.getFields()) {
            TypeSizeContext fieldTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(field.getType(), fieldTypeSizeCtxt);
            result.addField(fieldTypeSizeCtxt);
        }

        // champs variables
        {
            TypeSizeContext worst = new TypeSizeContext();

            for (VariantField union : type.getUnionFields()) {
                TypeSizeContext unionTypeSizeCtxt = new TypeSizeContext();

                computeTypeSize(union.getType(), unionTypeSizeCtxt);

                worst.rawSize = Math.max(worst.rawSize, unionTypeSizeCtxt.rawSize);
                worst.alignment = Math.max(worst.alignment, unionTypeSizeCtxt.alignment);
                worst.sizeof = Math.max(worst.sizeof, unionTypeSizeCtxt.sizeof);
            }

            // On aligne la section de champs variables. Sa pire taille
            // mémoire et son pire alignement peuvent provenir de deux champs
            // différents de l'union !
            worst.finalize();

            // On ajoute ce proto champ
            result.addField(worst);
        }

        result.finalize();
    }

    // Dimensionne un type string

    private static void computeStringSize(TypeDefinition type, TypeSizeContext result) {
        long maxNumber = type.getLength();

        TypeSizeContext cur_len_size = new TypeSizeContext();
        TypeSizeContext char_size = new TypeSizeContext();
        TypeSizeContext memory = new TypeSizeContext();
        TypeSizeContext raw = new TypeSizeContext();

        computePredefTypeSize("int32", cur_len_size);
        computePredefTypeSize("char8", char_size);

        // Compute serialized size
        raw.addField(cur_len_size); // current length
        raw.addArray(maxNumber + 1, char_size); // data

        // Compute memory size
        memory.addField(cur_len_size); // max length
        memory.addField(cur_len_size); // current length
        memory.addArray(maxNumber + 1, char_size); // data
        memory.finalize();

        // Merge both
        memory.rawSize = raw.rawSize;

        // Insert string inside more global type
        result.addField(memory);
        result.finalize();
    }

    // Dimensionne un type prédéfini

    static void computePredefTypeSize(String typeName, TypeSizeContext result) {
        if (typeName.equals("int8") || typeName.equals("uint8") || typeName.equals("char8") || typeName.equals("boolean8")
                || typeName.equals("uchar8")) {

            result.rawSize = 1;
            result.alignment = 1;
            result.sizeof = 1;
        }

        else if (typeName.equals("int16") || typeName.equals("uint16")) {
            result.rawSize = 2;
            result.alignment = 2;
            result.sizeof = 2;
        }

        else if (typeName.equals("int32") || typeName.equals("uint32") || typeName.equals("float32")) {
            result.rawSize = 4;
            result.alignment = 4;
            result.sizeof = 4;
        }

        else if (typeName.equals("int64") || typeName.equals("uint64") || typeName.equals("double64")) {
            result.rawSize = 8;
            result.alignment = 8;
            result.sizeof = 8;
        }

        else {
            throw new Error(typeName + ": unknown predef type");
        }
    }
}
