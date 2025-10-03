/* @file imagesampler_user_context.h
 * This is an example of a user defined User Module context
 */

 #if !defined(imagesampler_USER_CONTEXT_H)
 #define imagesampler_USER_CONTEXT_H

 #if defined(__cplusplus)
 extern "C" {  
 #endif /* __cplusplus */

 #include "image.h"
 #include "image_types.h"
 /* Container Types */
 #include "imagesampler_container_types.h"

 /* User Module Context structure example */
 typedef struct
 {
    /* declare the User Module Context "local" data here */

 } imagesampler_user_context;



 #if defined(__cplusplus)
 } 
 #endif /* __cplusplus */

 #endif  /* imagesampler_USER_CONTEXT_H */
