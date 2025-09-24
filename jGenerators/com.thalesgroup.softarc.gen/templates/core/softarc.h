/* Copyright (c) 2025 THALES -- All rights reserved */

#ifndef SARC_SOFTARC_MINIMAL_H
#define SARC_SOFTARC_MINIMAL_H

/* ===========================================================================
 * Predefined types
 * ======================================================================== */

typedef unsigned char SARC_boolean8;

/** Boolean value for logical false */

#define SARC_FALSE 0U


/** Boolean value for logical true */

#define SARC_TRUE 1U


/** Scalar type for characters - 1 byte long */

typedef char SARC_char8;


/** Scalar type for 1-byte long signed integers */

typedef signed char SARC_int8;


/** Scalar type for 1-byte long unsigned integers */

typedef unsigned char SARC_uint8;


/** Scalar type for 2-byte long signed integers */

typedef signed short int SARC_int16;


/** Scalar type for 2-byte long unsigned integers */

typedef unsigned short int SARC_uint16;


/** Scalar type for 4-byte long signed integers */

typedef signed int SARC_int32;


/** Scalar type for 4-byte long unsigned integers */

typedef unsigned int SARC_uint32;


/** Scalar type for 8-byte long signed integers */

typedef signed long long SARC_int64;


/** Scalar type for 8-byte long unsigned integers */

typedef unsigned long long SARC_uint64;


/** Scalar type for IEEE-754 simple precision floating point numbers */

typedef float SARC_float32;


/** Scalar type for IEEE-754 double precision floating point numbers */

typedef double SARC_double64;


typedef SARC_uint32 SARC_Counter;

/**
 * Special value deliberately out of validity range. As value is subject to
 * future evolution, user should not rely directly on it, but on symbol
 * instead.
 */
#define SARC_COUNTER_INVALID 0xDEADFEED

/**
 * Executable states - 4 bytes
 */
typedef SARC_uint32 SARC_ExecutableStates;


/** Tag for unavailable executable status */
#define SARC_STATUS_EXECUTABLE_NULL 0


/** Executables commands - 4 bytes */
typedef SARC_uint32 SARC_ExecutablesCommands;
#define SARC_PANEL_COMMAND_NULL 0
#define SARC_PANEL_COMMAND_LAUNCH 1
#define SARC_PANEL_COMMAND_KILL 2


#define SARC_boolean8_initialize(x) *(x)=0
#define SARC_char8_initialize(x) *(x)=0
#define SARC_int8_initialize(x) *(x)=0
#define SARC_uint8_initialize(x) *(x)=0
#define SARC_int16_initialize(x) *(x)=0
#define SARC_uint16_initialize(x) *(x)=0
#define SARC_int32_initialize(x) *(x)=0
#define SARC_uint32_initialize(x) *(x)=0
#define SARC_int64_initialize(x) *(x)=0
#define SARC_uint64_initialize(x) *(x)=0
#define SARC_float32_initialize(x) *(x)=0
#define SARC_double64_initialize(x) *(x)=0


/* ===========================================================================
 * Common
 * ======================================================================== */

typedef enum
{
  /** Used when function behaved as expected */
  SARC_SUCCESS = 0,
  /** Most of the time, abnormal behaviour results in a FAILURE */
  SARC_FAILURE = 1,
  /** When a function returns because its execution time slot has expired */
  SARC_TIMEOUT = 7,
  /** Alias for SARC_SUCCESS - deprecated */
  SARC_OK = SARC_SUCCESS,
  /** Alias for SARC_FAILURE - deprecated */
  SARC_KO = SARC_FAILURE,
  /** Used by SOFTARC container functions to link FAILURE to its inputs */
  SARC_INVALID_IN_PARAMETER = 2,
  /** Used by SOFTARC container functions to link FAILURE to its outputs */
  SARC_INVALID_OUT_PARAMETER = 3,
  /**
   * Used by SOFTARC container functions regarding data management to link
   * FAILURE to its inputs
   */
  SARC_INVALID_DATA = 4
} SARC_Ecode;

/**
 * This enumerated type describes the possible ways a data
 * handle can be initialized.
 */
typedef enum
{
    /**
     * The memory area pointed by the handle has no specific value
     * Use this by default in order to have better performances.
     */
  SARC_DATA_NO_VALUE,

    /**
     * The memory area pointed by the handle is filled with the
     * current data value. This mode has lower performances, but
     * allows access the current data version before modifying it.
     */
  SARC_DATA_CURRENT_VERSION
} SARC_DataValue;


/** Reference position used when moving the reading head */
typedef enum
{
 /** Beginning of PINFO */
  SARC_PINFO_ORIGIN_START,
  /** Current position */
  SARC_PINFO_ORIGIN_CURRENT,
  /** End of PINFO */
  SARC_PINFO_ORIGIN_END
} SARC_PInfoOrigin;

typedef SARC_uint32 SARC_MwSize;


/* ===========================================================================
 * Traces
 * ======================================================================== */

typedef enum
{
  /** NONE is not used in actual traces */
  SARC_TRACE_NONE = 0,
  /** CRITICAL trace level */
  SARC_TRACE_CRITICAL = 1,
  /** ERROR trace level */
  SARC_TRACE_ERROR = 2,
  /** WARNING trace level */
  SARC_TRACE_WARNING = 3,
  /** INFO trace level */
  SARC_TRACE_INFO = 4,
  /** DEBUG trace level */
  SARC_TRACE_DEBUG = 5,
  /** (deprecated) TRACE trace level */
  SARC_TRACE_TRACE = 6
} SARC_TraceLevel;

/* ===========================================================================
 * Predefined types used to manage the life-cycle of
 * any component Instance.
 * ======================================================================== */

/**
 * State of the life-cycle FSM - 4 bytes
 */

typedef SARC_uint32 SARC_LifeCycleState;

typedef struct
{
  SARC_LifeCycleState state;
  SARC_boolean8 is_state_changed;
} SARC_LifeCycleStateStatus;

/**
 * Transition of the life-cycle FSM
 */

typedef SARC_uint32 SARC_LifeCycleShift;

/** Special value to illustrate the lack of transition */
#define SARC_LIFE_CYCLE_SHIFT_NONE 0x00

/** UNAVAILABLE Instance state */
#define SARC_LIFE_CYCLE_STATE_UNAVAILABLE 0x00

/* Concrete states */

/** IDLE Instance state */
#define SARC_LIFE_CYCLE_STATE_IDLE 0x01
/** READY Instance state */
#define SARC_LIFE_CYCLE_STATE_READY 0x02
/** RUNNING Instance state */
#define SARC_LIFE_CYCLE_STATE_RUNNING 0x03


/* Transitions */

/** RISE transition */
#define SARC_LIFE_CYCLE_SHIFT_RISE 0x01
/** INITIALIZE transition */
#define SARC_LIFE_CYCLE_SHIFT_INITIALIZE 0x02
/** START transition */
#define SARC_LIFE_CYCLE_SHIFT_START 0x03
/** RESET transition */
#define SARC_LIFE_CYCLE_SHIFT_RESET 0x04
/** STOP transition */
#define SARC_LIFE_CYCLE_SHIFT_STOP 0x05
/** SHUTDOWN transition */
#define SARC_LIFE_CYCLE_SHIFT_SHUTDOWN 0x06
/** KILL transition */
#define SARC_LIFE_CYCLE_SHIFT_KILL 0x07



#endif /* SARC_SOFTARC_MINIMAL_H */
