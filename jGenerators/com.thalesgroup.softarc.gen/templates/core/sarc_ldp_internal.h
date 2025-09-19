/* Copyright (c) 2025 THALES -- All rights reserved */

#ifndef SARC_LDP_INTERNAL_H
#define SARC_LDP_INTERNAL_H

#include "softarc.h"

#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>             //malloc
#include <errno.h>
#include <pthread.h>
#include <stdarg.h>
#include <time.h>
#include <assert.h>
#include <sys/socket.h>
#include <sys/un.h>             // sockaddr_un
#include <sys/time.h>           //struct val
#include <fcntl.h>

/** Max size (in bytes) for the total size of the parameters of an operation (event, data, request or response) */
#define SARC_LDP_MAX_OPERATION_SIZE 256*1024

#define SARC_MIN_TIMEOUT 300 //in us


typedef char SARC_Byte;

/** Special Task id for invalid Task */

#define SARC_NO_TASK 0xFFFFFFFF


/* Special value to identify an invalid index in a container. */
#define SARC_NONE 0xFFFFFFFF



/**
 * This service shall determine the state reached when trying to
 * apply one shift on a specific state. If the shift is not allowed, the state
 * stays unchanged.
 * @param[in] state initial component state
 * @param[in] shift proposed shift
 * @retval result final component state
 */

SARC_LifeCycleStateStatus SARC_life_cycle_next_state (SARC_LifeCycleState
                                                      state,
                                                      SARC_LifeCycleShift
                                                      shift);


/**
 * This service shall determine natural shift to apply right after
 * commanded ones.
 * @param[in] shift received shift
 * @retval result shift to send, or SARC_LIFE_CYCLE_SHIFT_NONE
 */

SARC_LifeCycleShift SARC_life_cycle_shift_response (SARC_LifeCycleShift
                                                    shift);

extern const char *SARC_table_name_of_life_cycle_state[4];
extern const char *SARC_table_name_of_life_cycle_shift[8];

/* ===========================================================================
 * Simplified serialization API (without context, no error handling)
 * ======================================================================== */

void SARC_serial_copy_or_swap_2bytes (void *dest, const void *src);

void SARC_serial_copy_or_swap_4bytes (void *dest, const void *src);

void SARC_serial_copy_or_swap_8bytes (void *dest, const void *src);



/* ===========================================================================
 * Serialization
 * ======================================================================== */

/**
 * Serialization context
 */

typedef struct
{
  /** Byte buffer used to build stream of bytes version of data */
  SARC_char8 *buffer;
  /** Number of bytes already used in the stream of bytes */
  SARC_MwSize pos;
  /** True iff at least one error has been detected by a serialization
   * routine */
  SARC_boolean8 error;
  /** True iff last called serialization routine detected an error */
  SARC_boolean8 local_error;
} SARC_SerializationContext;


void
SARC_serial_start_serialize (SARC_SerializationContext * s, void *buffer);

void
SARC_serial_check_serialize (SARC_SerializationContext * s,
                             const SARC_char8 * name);

void
SARC_serial_serialize_1byte (SARC_SerializationContext * s, const void *src);

void
SARC_serial_serialize_2bytes (SARC_SerializationContext * s, const void *src);

void
SARC_serial_serialize_4bytes (SARC_SerializationContext * s, const void *src);

void
SARC_serial_serialize_8bytes (SARC_SerializationContext * s, const void *src);

/* ===========================================================================
 * Deserialization
 * ======================================================================== */

/**
 * Deserialization context
 */

typedef struct
{
  /** Byte buffer storing the stream of bytes version of data */
  const SARC_char8 *buffer;
  /** Position of the reading head in the byte buffer */
  SARC_MwSize pos;
  /** Size, in bytes, of the byte buffer */
  SARC_MwSize raw_size;
  /**
   * True iff at least one error has been detected during deserialization
   * process
   */
  SARC_boolean8 error;
  /**
   * Flag to determine whether or not check procedure shall react on underflow
   * situations (too many bytes in the byte buffer)
   */
  SARC_boolean8 check_underflow;
} SARC_DeserializationContext;

void
SARC_serial_start_deserialize (SARC_DeserializationContext * s,
                               const void *buffer, SARC_MwSize size);

void
SARC_serial_check_deserialize (SARC_DeserializationContext * s,
                               const SARC_char8 * name);

void
SARC_serial_deserialize_1byte (SARC_DeserializationContext * s, void *dest);

void
SARC_serial_deserialize_2bytes (SARC_DeserializationContext * s, void *dest);

void
SARC_serial_deserialize_4bytes (SARC_DeserializationContext * s, void *dest);

void
SARC_serial_deserialize_8bytes (SARC_DeserializationContext * s, void *dest);


/* ===========================================================================
 * Base types serialization macros
 * ======================================================================== */

/**
 * This service shall transform int8 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable int8 memory area
 */

#define SARC_int8_serialize(s,src) SARC_serial_serialize_1byte((s),(src))


/**
 * This service shall transform uint8 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable uint8 memory area
 */

#define SARC_uint8_serialize(s,src) SARC_serial_serialize_1byte((s),(src))


/**
 * This service shall transform boolean8 data into a stream of
 * bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable boolean8 memory area
 */

#define SARC_boolean8_serialize(s,src) SARC_serial_serialize_1byte((s),(src))


/**
 * This service shall transform char8 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable char8 memory area
 */

#define SARC_char8_serialize(s,src) SARC_serial_serialize_1byte((s),(src))


/**
 * This service shall transform int16 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable int16 memory area
 */

#define SARC_int16_serialize(s,src) SARC_serial_serialize_2bytes((s),(src))


/**
 * This service shall transform uint16 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable uint16 memory area
 */

#define SARC_uint16_serialize(s,src) SARC_serial_serialize_2bytes((s),(src))


/**
 * This service shall transform int32 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable int32 memory area
 */

#define SARC_int32_serialize(s,src) SARC_serial_serialize_4bytes((s),(src))


/**
 * This service shall transform uint32 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable uint32 memory area
 */

#define SARC_uint32_serialize(s,src) SARC_serial_serialize_4bytes((s),(src))


/**
 * This service shall transform float32 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable float32 memory area
 */

#define SARC_float32_serialize(s,src) SARC_serial_serialize_4bytes((s),(src))


/**
 * This service shall transform double64 data into a stream of
 * bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable double64 memory area
 */

#define SARC_double64_serialize(s,src) SARC_serial_serialize_8bytes((s),(src))


/**
 * This service shall transform int64 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable int64 memory area
 */

#define SARC_int64_serialize(s,src) SARC_serial_serialize_8bytes((s),(src))


/**
 * This service shall transform uint64 data into a stream of bytes.
 * @param[in,out] s a SARC_SerializationContext serialization context
 * @param[in] src a pointer to a readable uint64 memory area
 */

#define SARC_uint64_serialize(s,src) SARC_serial_serialize_8bytes((s),(src))


/* ===========================================================================
 * Base types deserialization macros
 * ======================================================================== */

/**
 * This service shall transform the next byte of a stream of bytes
 * into int8 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable int8 memory area
 */

#define SARC_int8_deserialize(s,dest) SARC_serial_deserialize_1byte((s),(dest))


/**
 * This service shall transform the next byte of a stream of bytes
 * into uint8 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable uint8 memory area
 */

#define SARC_uint8_deserialize(s,dest) SARC_serial_deserialize_1byte((s),(dest))


/**
 * This service shall transform the next byte of a stream of bytes
 * into boolean8 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable boolean8 memory area
 */

#define SARC_boolean8_deserialize(s,dest) SARC_serial_deserialize_1byte((s),(dest))


/**
 * This service shall transform the next byte of a stream of bytes
 * into char8 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable char8 memory area
 */

#define SARC_char8_deserialize(s,dest) SARC_serial_deserialize_1byte((s),(dest))


/**
 * This service shall transform the next 2 bytes of a stream of bytes
 * into int16 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable int16 memory area
 */

#define SARC_int16_deserialize(s,dest) SARC_serial_deserialize_2bytes((s),(dest))


/**
 * This service shall transform the next 2 bytes of a stream of bytes
 * into uint16 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable uint16 memory area
 */

#define SARC_uint16_deserialize(s,dest) SARC_serial_deserialize_2bytes((s),(dest))


/**
 * This service shall transform the next 4 bytes of a stream of bytes
 * into int32 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable int32 memory area
 */

#define SARC_int32_deserialize(s,dest) SARC_serial_deserialize_4bytes((s),(dest))


/**
 * This service shall transform the next 4 bytes of a stream of bytes
 * into uint32 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable uint32 memory area
 */

#define SARC_uint32_deserialize(s,dest) SARC_serial_deserialize_4bytes((s),(dest))


/**
 * This service shall transform the next 4 bytes of a stream of bytes
 * into float32 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable float32 memory area
 */

#define SARC_float32_deserialize(s,dest) SARC_serial_deserialize_4bytes((s),(dest))


/**
 * This service shall transform the next 8 bytes of a stream of bytes
 * into double64 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable double64 memory area
 */

#define SARC_double64_deserialize(s,dest) SARC_serial_deserialize_8bytes((s),(dest))


/**
 * This service shall transform the next 8 bytes of a stream of bytes
 * into int64 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable int64 memory area
 */

#define SARC_int64_deserialize(s,dest) SARC_serial_deserialize_8bytes((s),(dest))


/**
 * This service shall transform the next 8 bytes of a stream of bytes
 * into uint64 data.
 * @param[in,out] s a SARC_DeserializationContext deserialization context
 * @param[in] dest a pointer to a writable uint64 memory area
 */

#define SARC_uint64_deserialize(s,dest) SARC_serial_deserialize_8bytes((s),(dest))


/**
 * Identifier of the encountered error
 */

typedef SARC_uint16 SARC_ErrorCode;

/* errors raised by middleware */

#define SARC_ERROR_LOCAL_EXEC_KILLED 0x1001
#define SARC_ERROR_DESERIAL_OVERFLOW 0x1002
#define SARC_ERROR_DESERIAL_UNDERFLOW 0x1003
#define SARC_ERROR_SERIAL_CHECK 0x1004
#define SARC_ERROR_SERIAL_OVERFLOW        0x1005

/* errors raised by generated code */

#define SARC_ERROR_UNKNOWN 0x0000
#define SARC_ERROR_FIFOSIZE_OVERFLOW 0x0001
#define SARC_ERROR_MAXVERSION_OVERFLOW 0x0002
#define SARC_ERROR_MAXDEFERRED_OVERFLOW 0x0003
#define SARC_ERROR_MAXREQUESTS_OVERFLOW 0x0004
#define SARC_ERROR_UNKNOWN_OPERATION_ID 0x0010

/* verify that a boolean value respects the
 * logical range of its enumerated type. */
SARC_boolean8 SARC_boolean8_check (const SARC_boolean8 * value,
                                   SARC_char8 * msg);

void
SARC_error_raise (SARC_ErrorCode code, SARC_int32 i1,
                  SARC_int32 i2, const SARC_char8 * s1);

void
SARC_trace_error (SARC_TraceLevel level, SARC_uint32 id,
                  const SARC_char8 * message);


/*=============================================================================
 * Predefined Operations
 *=============================================================================*/

#define SARC_OP_LIFECYCLE_COMMAND 33
// followed by: instance_id, command

#define SARC_OP_LIFECYCLE_INFO 34
// followed by: instance_id, command

#define SARC_OP_REQUEST_REPONSE_TIMEOUT 35
// followed by: operation_id, request_id

#define SARC_OP_TIMED_MESSAGE 36
// followed by: timeout (struct timeval), message payload (variable size)


/*=============================================================================
 * LDP Types
 *=============================================================================*/


typedef struct
{
  void *current_value;
  SARC_int32 ref;
} SARC_op_data;

typedef struct SARC_timed_message
{
  SARC_char8 *message;
  SARC_uint32 size;
  SARC_int64 ts;
  struct SARC_timed_message *next;
} SARC_timed_message;

/**
 * Deferred services caller characteristics
 */
typedef struct
{
  /**
   * Request identifier from the client point-of-view
   */
  SARC_uint32 client_req_id;
  /**
   * Route index corresponding to the response
   */
  SARC_uint32 callback_id;
  /**
  * Task client id
  */
  SARC_uint32 thread_id;
} SARC_RequestInfo;

typedef void SARC_entrypoint_ptr ();

/* PINFOs */
typedef struct
{
  /** Operating system file handler */
  int fd;
  /** Size of the file at opening time */
  SARC_uint32 size;
  /** Position of the reading head */
  SARC_uint32 head;
} SARC_Pinfo;

/* Tasks */
typedef struct
{
  SARC_boolean8 is_started;
  pthread_t thread;
} SARC_Task;


/*=============================================================================
 * Helper functions
 *=============================================================================*/

void SARC_init_pinfo (SARC_Pinfo * pinfo, char *path, char *name);

void SARC_close_pinfo (SARC_Pinfo * pinfo);

SARC_Ecode SARC_read_pinfo(char* pinfo_name, SARC_Pinfo* pinfo, void* memory_address, SARC_MwSize capacity, SARC_MwSize* out_size);

SARC_Ecode SARC_seek_pinfo(char* pinfo_name, SARC_Pinfo* pinfo, SARC_int32 offset, SARC_PInfoOrigin origin, SARC_MwSize* position);

void SARC_init_task_index (void);

SARC_int32 SARC_task_start (SARC_uint32 _id, void (*routine));

SARC_uint32 SARC_task_get_index (void);

void
SARC_gen_handle_lifecycle_command (int instanceId,
                                   SARC_LifeCycleState * state,
                                   SARC_LifeCycleShift command,
                                   SARC_entrypoint_ptr *
                                   lifecycle_entrypoints[5]);

void
SARC_gen_send_lifecycle_command (SARC_int32 instance,
                                 SARC_LifeCycleShift command);

void SARC_launcher ();

void SARC_log_info (const char *sarc_format, ...);

void SARC_log_warning (const char *sarc_format, ...);

void SARC_log_error (const char *sarc_format, ...);

struct timeval SARC_timed_message_get_next_deadline (SARC_timed_message **
                                                     head);

void
SARC_timed_message_add (SARC_timed_message ** head, const void *message,
                        SARC_uint32 size, SARC_int64 ts);

SARC_timed_message*
SARC_timed_message_delete (SARC_timed_message ** head,
                           SARC_timed_message * message_to_delete);

SARC_int64 SARC_get_local_time ();

int SARC_timed_message_trigger_sendto (SARC_int32 oper_id, SARC_int64 timeout,
                                       const struct sockaddr_un *addr);


SARC_Ecode SARC_map_initialize (void *address, SARC_uint32 size,
                                SARC_uint32 capacity, SARC_uint32 value_size);

SARC_Ecode SARC_map_add (void *address, SARC_int64 key, const void *value);

SARC_Ecode SARC_map_remove (void *address, SARC_int64 key, void *value);

#endif
