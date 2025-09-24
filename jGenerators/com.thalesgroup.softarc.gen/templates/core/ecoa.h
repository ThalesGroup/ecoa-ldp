/*
 * @file ECOA.h
 */

/*  This is a compilable ISO C99 specification of the generic ECOA types,   */
/*  derived from the C binding specification.                               */

/*  The declarations of the types given below are taken from the            */
/*  standard, as are the enum types and the names of the others types.      */
/*  Unless specified as implementation dependent, the values specified in   */
/*  this appendix should be implemented as defined.                         */


#ifndef __ECOA_H__
#define __ECOA_H__

#if defined(__cplusplus)
extern "C"
{
#endif                          /* __cplusplus */

/* ECOA:boolean8 */
  typedef unsigned char ECOA__boolean8;
#define ECOA__TRUE  (1)
#define ECOA__FALSE (0)

/* ECOA:int8 */
  typedef char ECOA__int8;
#define ECOA__INT8_MIN (-127)
#define ECOA__INT8_MAX ( 127)

/* ECOA:char8 */
  typedef char ECOA__char8;
#define ECOA__CHAR8_MIN (0)
#define ECOA__CHAR8_MAX (127)

/* ECOA:byte */
  typedef unsigned char ECOA__byte;
#define ECOA__BYTE_MIN (0)
#define ECOA__BYTE_MAX (255)

/* ECOA:int16 */
  typedef short int ECOA__int16;
#define ECOA__INT16_MIN (-32767)
#define ECOA__INT16_MAX ( 32767)

/* ECOA:int32 */
  typedef int ECOA__int32;
#define ECOA__INT32_MIN (-2147483647L)
#define ECOA__INT32_MAX ( 2147483647L)


/* ECOA:uint8 */
  typedef unsigned char ECOA__uint8;
#define ECOA__UINT8_MIN (0)
#define ECOA__UINT8_MAX (255)

/* ECOA:uint16 */
  typedef unsigned short int ECOA__uint16;
#define ECOA__UINT16_MIN (0)
#define ECOA__UINT16_MAX (65535)

/* ECOA:uint32 */
  typedef unsigned int ECOA__uint32;
#define ECOA__UINT32_MIN (0LU)
#define ECOA__UINT32_MAX (4294967295LU)

/* ECOA:float32 */
  typedef float ECOA__float32;
#define ECOA__FLOAT32_MIN (-3.402823466e+38F)
#define ECOA__FLOAT32_MAX ( 3.402823466e+38F)

/* ECOA:double64 */
  typedef double ECOA__double64;
#define ECOA__DOUBLE64_MIN (-1.7976931348623157e+308)
#define ECOA__DOUBLE64_MAX ( 1.7976931348623157e+308)

#if defined(ECOA_64BIT_SUPPORT)

/* ECOA:int64 */
  typedef long long int ECOA__int64;
#define ECOA__INT64_MIN (-9223372036854775807LL)
#define ECOA__INT64_MAX ( 9223372036854775807LL)

/* ECOA:uint64 */
  typedef unsigned long long int ECOA__uint64;
#define ECOA__UINT64_MIN (0LLU)
#define ECOA__UINT64_MAX (18446744073709551615LLU)

#endif                          /* ECOA_64BIT_SUPPORT */


/* ECOA:error_id */
  typedef ECOA__uint32 ECOA__error_id;

/* ECOA:asset_id */
  typedef ECOA__uint32 ECOA__asset_id;

/* ECOA:asset_type */
  typedef ECOA__uint32 ECOA__asset_type;
#define ECOA__asset_type_COMPONENT         (0)
#define ECOA__asset_type_PROTECTION_DOMAIN (1)
#define ECOA__asset_type_NODE              (2)
#define ECOA__asset_type_PLATFORM          (3)
#define ECOA__asset_type_SERVICE           (4)
#define ECOA__asset_type_DEPLOYMENT        (5)

/* ECOA:error_type */
  typedef ECOA__uint32 ECOA__error_type;
#define ECOA__error_type_RESOURCE_NOT_AVAILABLE (0)
#define ECOA__error_type_UNAVAILABLE            (1)
#define ECOA__error_type_MEMORY_VIOLATION       (2)
#define ECOA__error_type_NUMERICAL_ERROR        (3)
#define ECOA__error_type_ILLEGAL_INSTRUCTION    (4)
#define ECOA__error_type_STACK_OVERFLOW         (5)
#define ECOA__error_type_DEADLINE_VIOLATION     (6)
#define ECOA__error_type_OVERFLOW               (7)
#define ECOA__error_type_UNDERFLOW              (8)
#define ECOA__error_type_ILLEGAL_INPUT_ARGS     (9)
#define ECOA__error_type_ILLEGAL_OUTPUT_ARGS    (10)
#define ECOA__error_type_ERROR                  (11)
#define ECOA__error_type_FATAL_ERROR            (12)
#define ECOA__error_type_HARDWARE_FAULT         (13)
#define ECOA__error_type_POWER_FAIL             (14)
#define ECOA__error_type_COMMUNICATION_ERROR    (15)
#define ECOA__error_type_INVALID_CONFIG         (16)
#define ECOA__error_type_INITIALISATION_PROBLEM (17)
#define ECOA__error_type_CLOCK_UNSYNCHRONIZED   (18)
#define ECOA__error_type_UNKNOWN_OPERATION      (19)
#define ECOA__error_type_OPERATION_OVERRATED    (20)
#define ECOA__error_type_OPERATION_UNDERRATED   (21)

/* ECOA:recovery_action_type */
  typedef ECOA__uint32 ECOA__recovery_action_type;
#define ECOA__recovery_action_type_SHUTDOWN          (0)
#define ECOA__recovery_action_type_COLD_RESTART      (1)
#define ECOA__recovery_action_type_WARM_RESTART      (2)
#define ECOA__recovery_action_type_CHANGE_DEPLOYMENT (3)

/* ECOA:seek_whence_type */
  typedef ECOA__uint32 ECOA__seek_whence_type;
#define ECOA__seek_whence_type_SEEK_SET (0)
#define ECOA__seek_whence_type_SEEK_CUR (1)
#define ECOA__seek_whence_type_SEEK_END (2)


// To be fixed in the standard:
// the type name should be lowercase (ex: ECOA__log_MAXSIZE)
#define ECOA__LOG_MAXSIZE 256
#define ECOA__PINFO_FILENAME_MAXSIZE 256

#define ECOA__boolean8__initialize(x) *(x)=0
#define ECOA__char8__initialize(x) *(x)=0
#define ECOA__int8__initialize(x) *(x)=0
#define ECOA__uint8__initialize(x) *(x)=0
#define ECOA__int16__initialize(x) *(x)=0
#define ECOA__uint16__initialize(x) *(x)=0
#define ECOA__int32__initialize(x) *(x)=0
#define ECOA__uint32__initialize(x) *(x)=0
#define ECOA__int64__initialize(x) *(x)=0
#define ECOA__uint64__initialize(x) *(x)=0
#define ECOA__float32__initialize(x) *(x)=0
#define ECOA__double64__initialize(x) *(x)=0

/* ECOA.return_status */
  typedef ECOA__uint32 ECOA__return_status;
#define ECOA__return_status_OK (0)
#define ECOA__return_status_INVALID_HANDLE (1)
#define ECOA__return_status_DATA_NOT_INITIALIZED (2)
#define ECOA__return_status_NO_DATA (3)
#define ECOA__return_status_INVALID_IDENTIFIER (4)
#define ECOA__return_status_NO_RESPONSE (5)
#define ECOA__return_status_OPERATION_ALREADY_PENDING (6)
#define ECOA__return_status_CLOCK_UNSYNCHRONIZED (7)
#define ECOA__return_status_RESOURCE_NOT_AVAILABLE (8)
#define ECOA__return_status_OPERATION_NOT_AVAILABLE (9)
#define ECOA__return_status_INVALID_PARAMETER (10)

/* ECOA.hr_time */
  typedef struct
  {
    ECOA__uint32 seconds;       /* Seconds */
    ECOA__uint32 nanoseconds;   /* Nanoseconds */
  } ECOA__hr_time;
/* ECOA.global_time */
  typedef struct
  {
    ECOA__uint32 seconds;       /* Seconds */
    ECOA__uint32 nanoseconds;   /* Nanoseconds */
  } ECOA__global_time;
/* ECOA.duration */
  typedef struct
  {
    ECOA__uint32 seconds;       /* Seconds */
    ECOA__uint32 nanoseconds;   /* Nanoseconds */
  } ECOA__duration;
/* ECOA.log */
  typedef struct
  {
    ECOA__uint32 current_size;
    ECOA__char8 data[ECOA__LOG_MAXSIZE];
  } ECOA__log;


#if defined(__cplusplus)
}
#endif                          /* __cplusplus */

#endif                          /* __ECOA_H__ */
