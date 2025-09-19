/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

public class DataVrSizer {

    // cf. sarc_datavr_internal.h
    static final long DATAVR_HEADER_SIZE = 24;
    static final long DATAVR_CELL_SIZE =
            16; // TODO : à passer à 12 à la suppression du variant ATOMIC_OFF

    // cf. sarc_datasv_internal.h
    static final long DATASV_HEADER_SIZE = 16;
    // Plus petit multiple de 'alignment' qui soit supérieur ou égal à 'size'

    public static final long DATAVR_MAX_SIZE = 4294967295L;
    // justification: SARC_MwSize non signé 32 bits

    private static long aligned_size(long size, long alignment) {
        long result;

        result = alignment * ((size + alignment - 1) / alignment);

        return result;
    }

    // Nombre d'octets nécessaires pour gérer en mémoire partagée
    // 'numberOfData' versions d'une donnée faisant 'dataSize' octets

    public static long computeDataVrShmGlobalSize(long numberOfData, long dataSize) {
        long result;

        // Formule appliquée par sarc_datavr_ATOMIC_ON.c, ligne 61
        // À noter que le calcul reste juste pour le variant ATOMIC_OFF, qui
        // s'appuie de son côté sur le fait que «SARC_DataVRCell» occupe un
        // multiple de 8 octets.

        // La formulation suivante est juste et représente le découpage en
        // trois blocs, quelque soit la valeur du variant ATOMIC :
        // - L'entête seul, aligné sur 8 octets ;
        // - La table des nœuds, alignée globalement sur 8 octets ;
        // - Les blocs, chacun étant aligné sur 8 octets.

        result = aligned_size(DATAVR_HEADER_SIZE, 8);
        result += aligned_size(numberOfData * DATAVR_CELL_SIZE, 8);
        result += numberOfData * aligned_size(dataSize, 8);

        return result;
    }

    /**
     * Nombre d'octets nécessaires pour gérer en mémoire partagée une donnée gérée selon la méthode
     * DataSV
     */
    public static long computeDataSvShmGlobalSize(long dataSize) {
        long result = 0;

        // Formule appliquée par sarc_datasv.c, ligne 28
        result = DATASV_HEADER_SIZE + dataSize;

        return result;
    }
}
