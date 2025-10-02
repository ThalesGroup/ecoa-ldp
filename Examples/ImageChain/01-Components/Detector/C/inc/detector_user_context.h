#ifndef _detector_USER_CONTEXT_H
#define _detector_USER_CONTEXT_H

#include "detector_types.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct
{
  SARC_int32 dummy; /* this field should be replaced by real component context fields, if needed */
} detector_user_context;

#ifdef __cplusplus
}
#endif
#endif /* _detector_USER_CONTEXT_H */
