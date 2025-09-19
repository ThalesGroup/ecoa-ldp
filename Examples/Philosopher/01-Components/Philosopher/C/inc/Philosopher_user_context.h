/* Copyright (c) 2025 THALES -- All rights reserved */

/* @file Philosopher_user_context.h
 * This is an example of a user defined User Module context
 */

 #if !defined(Philosopher_USER_CONTEXT_H)
 #define Philosopher_USER_CONTEXT_H

 #if defined(__cplusplus)
 extern "C" {  
 #endif /* __cplusplus */

 /* Container Types */
 #include "Philosopher_container_types.h"

typedef enum philosopher__State {
   philosopher__State_UNDEFINED = 0,
   philosopher__State_GETTINGSTICKS = 1,
   philosopher__State_EATING = 2,
   philosopher__State_SURRENDERING = 3,
   philosopher__State_THINKING = 4
} philosopher__State;
 
 /* User Module Context structure example */
typedef struct
{
   philosopher__State PhiloState;
   ECOA__hr_time EatUntil;
   ECOA__hr_time ThinkUntil;
   ECOA__boolean8 HaveLeftStick;
   ECOA__boolean8 HaveRightStick;

} Philosopher_user_context;

 /* Warm Start Module Context structure example */
 typedef struct
 {
    /* declare the Warm Start Module Context data here */

 } Philosopher_warm_start_context;

 #if defined(__cplusplus)
 } 
 #endif /* __cplusplus */

 #endif  /* Philosopher_USER_CONTEXT_H */
