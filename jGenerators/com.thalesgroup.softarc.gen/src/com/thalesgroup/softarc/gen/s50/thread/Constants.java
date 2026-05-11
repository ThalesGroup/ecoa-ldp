package com.thalesgroup.softarc.gen.s50.thread;

public class Constants {

    static final String THREAD_PREFIX_EXTERNAL = "E_";
    public static final String THREAD_PREFIX_DISPATCH = "D_";
    
    static final int LINK_MAX_NUMBER_OF_PARTICIPANTS_MASK = 64;
    // - 1 for the reference
    static final int LINK_MAX_NUMBER_OF_PARTICIPANTS_MASK_DATA = 64 - 1;

    static final long max_message_size = 2L << 16;

    public static final long LOWEST_PRIORITY = 1;
    public static final long HIGHEST_PRIORITY = 35;

    /**
     * Amount of stack capacity consumed by the generated code, for each functional thread (96 kio)
     */
    public static final long SARC_THREAD_FRAMEWORK_STACK_SIZE = 98304;

    /** Stack size of the Softarc internal threads (128 kio) */
    public static final long SARC_DEFAULT_THREAD_STACK_SIZE = 131072;
    
}
