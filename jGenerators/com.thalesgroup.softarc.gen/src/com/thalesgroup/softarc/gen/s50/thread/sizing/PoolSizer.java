/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.gen.s50.thread.sizing;

// Classe utilitaire pour déterminer la taille minimale à allouer pour gérer
// un ensemble connu de données dans un 'pool'
// Le scénario classique est le suivant :
//
// ps = new PoolSizer();
// ps.init();
// ps.add_data(size1);
// ps.add_data(size2);
//  ...
// ps.add_data(sizeN);
// allocated_size = ps.get_size();
// capacity = ps.get_capacity();
// block_size = ps.get_block_size();
//

public class PoolSizer {
    // Algorithm retains largest data size and the number of data to manage,
    // to determine minimum size of the buffer that would host the pool

    // Size, in bytes, of SARC_PoolHeader MW type
    private final long header_size = 16;

    // Optional per-data overhead
    public long overhead = 0;

    // Number of different data managed by the pool
    public long data_count = 0;

    // Size, in bytes, of the largest managed data
    public long max_block_size = 0;

    // Reset internal state to start a new sizing.
    // WARNING: overhead is not reset to 0
    public void init() {
        this.data_count = 0;

        // At minimum, a block shall be large enough to store a SARC_Size.
        this.max_block_size = 8;
    }

    // Take into account the need for a data of 'size' bytes long
    public void add_data(long p_size) {
        long block_size = this.overhead + p_size;

        // Block size is rounded up to the next multiple of 8
        block_size += 7;
        block_size = block_size & (block_size ^ 7);

        // Only the largest block size is taken into account
        if (block_size > this.max_block_size) {
            this.max_block_size = block_size;
        }

        // Another data has to be taken into account
        this.data_count += 1;
    }

    // Minimum buffer size needed to manage all added data so far
    public long get_size() {
        long result = 0;

        result = this.header_size + this.get_capacity() * this.get_block_size();

        return result;
    }

    // Number of data to manage
    public long get_capacity() {
        return this.data_count;
    }

    // Size, in bytes, of each pool's block
    public long get_block_size() {
        return this.max_block_size;
    }

}