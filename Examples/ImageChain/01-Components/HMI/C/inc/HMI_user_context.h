#ifndef _HMI_USER_CONTEXT_H
#define _HMI_USER_CONTEXT_H

#include "HMI_types.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct
{
  SARC_int32 dummy; /* this field should be replaced by real component context fields, if needed */
} HMI_user_context;

#ifdef __cplusplus
}
#endif
#endif /* _HMI_USER_CONTEXT_H */
