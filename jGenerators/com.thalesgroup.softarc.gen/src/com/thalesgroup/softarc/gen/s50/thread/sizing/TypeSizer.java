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
        type.setSize(result.raw_size);
        type.setSizeof(result.sizeof);
    }

    // Dimensionne un tableau de taille fixe

    private static void computeFixedArraySize(TypeDefinition type, TypeSizeContext result) {
        long maxNumber = type.getArraySize();
        TypeSizeContext innerTypeSizeCtxt = new TypeSizeContext();

        // array elements
        computeTypeSize(type.getBaseType(), innerTypeSizeCtxt);
        result.add_array(maxNumber, innerTypeSizeCtxt);
    }

    // Dimensionne un tableau de taille variable

    private static void computeArraySize(TypeDefinition type, TypeSizeContext result) {
        long maxNumber = type.getArraySize();

        TypeSizeContext count_size = new TypeSizeContext();
        TypeSizeContext element_size = new TypeSizeContext();

        // array header : number of elements
        computePredefTypeSize("int32", count_size);
        result.add_field(count_size);

        // array elements
        computeTypeSize(type.getBaseType(), element_size);
        result.add_array(maxNumber, element_size);

        result.finalize();
    }

    // Dimensionne une liste

    private static void computeListSize(TypeDefinition type, TypeSizeContext result) {
        TypeSizeContext list = new TypeSizeContext();
        TypeSizeContext uint32 = new TypeSizeContext();
        TypeSizeContext item = new TypeSizeContext();
        long capacity = type.getArraySize();
        long raw_size = result.raw_size;

        computePredefTypeSize("uint32", uint32);

        // First, compute list size as a uint64 C array
        list.alignment = 8;
        list.add_array(5 + capacity, uint32);

        computeTypeSize(type.getBaseType(), item);
        list.add_array(capacity, item);

        list.finalize();

        // Then, insert such array in more global type
        result.add_field(list);
        result.finalize();

        // The index is not serialized
        result.raw_size = raw_size + 4 + item.raw_size * capacity;
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
        long raw_size = result.raw_size;

        computePredefTypeSize("uint32", uint32);
        computePredefTypeSize("uint64", uint64);
        computeTypeSize(type.getKeyType(), key);
        computeTypeSize(type.getBaseType(), value);

        // Map type is a difficult one: in memory, it shall be seen as an
        // array of uint64, but its serialized size depends on the size of the
        // keys

        // A map is implemented as a list of (uint64, value) items
        item.add_field(uint64);
        item.add_field(value);
        item.finalize();

        map.alignment = 8;
        map.add_array(7 + capacity, uint32);
        map.add_array(capacity, item);
        map.finalize();

        // Insert map inside more global type
        result.add_field(map);
        result.finalize();

        // The index is not serialized, and the size used by keys is specific
        result.raw_size = raw_size + uint32.raw_size + capacity * (key.raw_size + value.raw_size);
    }

    // Dimensionne une structure (sans discriminant)

    private static void computeRecordSize(TypeDefinition type, TypeSizeContext result) {
        // each field is append sequentially
        for (Parameter field : type.getFields()) {
            TypeSizeContext fieldTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(field.getType(), fieldTypeSizeCtxt);
            result.add_field(fieldTypeSizeCtxt);
        }

        result.finalize();
    }

    // Dimensionne une structure à discriminant

    private static void computeVariantRecordSize(TypeDefinition type, TypeSizeContext result) {
        // discriminant
        {
            TypeSizeContext selectTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(type.getBaseType(), selectTypeSizeCtxt);
            result.add_field(selectTypeSizeCtxt);
        }

        // champs fixes
        for (Parameter field : type.getFields()) {
            TypeSizeContext fieldTypeSizeCtxt = new TypeSizeContext();

            computeTypeSize(field.getType(), fieldTypeSizeCtxt);
            result.add_field(fieldTypeSizeCtxt);
        }

        // champs variables
        {
            TypeSizeContext worst = new TypeSizeContext();

            for (VariantField union : type.getUnionFields()) {
                TypeSizeContext unionTypeSizeCtxt = new TypeSizeContext();

                computeTypeSize(union.getType(), unionTypeSizeCtxt);

                worst.raw_size = Math.max(worst.raw_size, unionTypeSizeCtxt.raw_size);
                worst.alignment = Math.max(worst.alignment, unionTypeSizeCtxt.alignment);
                worst.sizeof = Math.max(worst.sizeof, unionTypeSizeCtxt.sizeof);
            }

            // On aligne la section de champs variables. Sa pire taille
            // mémoire et son pire alignement peuvent provenir de deux champs
            // différents de l'union !
            worst.finalize();

            // On ajoute ce proto champ
            result.add_field(worst);
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
        raw.add_field(cur_len_size); // current length
        raw.add_array(maxNumber + 1, char_size); // data

        // Compute memory size
        memory.add_field(cur_len_size); // max length
        memory.add_field(cur_len_size); // current length
        memory.add_array(maxNumber + 1, char_size); // data
        memory.finalize();

        // Merge both
        memory.raw_size = raw.raw_size;

        // Insert string inside more global type
        result.add_field(memory);
        result.finalize();
    }

    // Dimensionne un type prédéfini

    static void computePredefTypeSize(String typeName, TypeSizeContext result) {
        if (typeName.equals("int8") || typeName.equals("uint8") || typeName.equals("char8") || typeName.equals("boolean8")
                || typeName.equals("uchar8")) {

            result.raw_size = 1;
            result.alignment = 1;
            result.sizeof = 1;
        }

        else if (typeName.equals("int16") || typeName.equals("uint16")) {
            result.raw_size = 2;
            result.alignment = 2;
            result.sizeof = 2;
        }

        else if (typeName.equals("int32") || typeName.equals("uint32") || typeName.equals("float32")) {
            result.raw_size = 4;
            result.alignment = 4;
            result.sizeof = 4;
        }

        else if (typeName.equals("int64") || typeName.equals("uint64") || typeName.equals("double64")) {
            result.raw_size = 8;
            result.alignment = 8;
            result.sizeof = 8;
        }

        else {
            throw new Error(typeName + ": unknown predef type");
        }
    }
}
