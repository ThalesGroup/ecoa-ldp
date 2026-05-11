/* @file Sender_user_context.h
 * This is an example of a user defined User Module context
 */

 #if !defined(Sender_USER_CONTEXT_H)
 #define Sender_USER_CONTEXT_H

 #if defined(__cplusplus)
 extern "C" {  
 #endif /* __cplusplus */

 #include "TestLib.h"
 #include "TestLib_types.h"
 /* Container Types */
 #include "Sender_container_types.h"

 /* User Module Context structure example */
 typedef struct
 {
    /* declare the User Module Context "local" data here */
    int N;
 } Sender_user_context;



 #if defined(__cplusplus)
 } 
 #endif /* __cplusplus */

 #endif  /* Sender_USER_CONTEXT_H */
