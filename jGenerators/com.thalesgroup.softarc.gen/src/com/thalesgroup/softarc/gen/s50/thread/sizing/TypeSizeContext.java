/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

// Dimensions d'un type (alignement, taille en octets, etc.)
//
// Utilisation classique :
//
//   tsc = new TypeSizeContext();
//
//   tsc.addField( other_tsc );
//   tsc.addField( another_tsc );
//   ...
//   tsc.finalize();
//

public class TypeSizeContext {
    // Taille du type sérialisé (en nombre d'octets)
    public long rawSize;

    // Alignement du type : toute instance doit être alignée en mémoire sur un
    // multiple de cette valeur
    public long alignment;

    // Taille du type en mémoire (en nombre d'octets). Équivalent de sizeof()
    public long sizeof;

    public TypeSizeContext() {
        rawSize = 0;
        alignment = 1;
        sizeof = 0;
    }

    // Introduit dans la structure dont on élabore le contexte un champ de
    // contexte correspondant

    public void addField(TypeSizeContext field) {
        rawSize += field.rawSize;
        align(field.alignment);
        alignment = Math.max(alignment, field.alignment);
        sizeof += field.sizeof;
    }

    // Introduit dans la structure dont on élabore le contexte un tableau
    // de caractéristiques correspondantes

    public void addArray(long nbElements, TypeSizeContext element) {
        rawSize += nbElements * element.rawSize;
        align(element.alignment);
        alignment = Math.max(alignment, element.alignment);
        sizeof += nbElements * element.sizeof;
    }

    // Assure que la taille en mémoire est bien un multiple de l'alignement

    public void finalize() {
        align(alignment);
    }

    // Ajoute le nombre minimal d'octets nécessaires pour assurer que la
    // taille en mémoire soit multiple de l'alignement

    private void align(long alignment) {
        sizeof = alignment * ((sizeof + alignment - 1) / alignment);
    }

}
