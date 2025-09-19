/* Copyright (c) 2025 THALES -- All rights reserved */

/* @file Table_user_context.h
 * This is an example of a user defined User Module context
 */

 #if !defined(Table_USER_CONTEXT_H)
 #define Table_USER_CONTEXT_H

 #if defined(__cplusplus)
 extern "C" {  
 #endif /* __cplusplus */

 /* Container Types */
 #include "Table_container_types.h"

 #define FREE -1

 /* User Module Context structure example */
 typedef struct
 {
    /* declare the User Module Context "local" data here */
   ECOA__int32 Stick[5];
 } Table_user_context;

 /* Warm Start Module Context structure example */
 typedef struct
 {
    /* declare the Warm Start Module Context data here */

 } Table_warm_start_context;

 #if defined(__cplusplus)
 } 
 #endif /* __cplusplus */

 #endif  /* Table_USER_CONTEXT_H */
