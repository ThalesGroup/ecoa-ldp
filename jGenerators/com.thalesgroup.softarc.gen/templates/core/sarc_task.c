/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp.h"

void
SARC_init_task_index (void)
{
  SARC_int32 _id;
  for (_id = 0; _id < SARC_MAX_NB_TASK; ++_id)
    {
      SARC_task_index[_id].is_started = SARC_FALSE;
      SARC_task_index[_id].thread = SARC_NO_TASK;
    }
}

SARC_uint32
SARC_task_get_index (void)
{
  SARC_int32 _result = SARC_NO_TASK;
  SARC_uint32 _id;
  pthread_t _thread;
  _thread = pthread_self ();
  for (_id = 0; _id < SARC_MAX_NB_TASK; ++_id)
    {
      if (SARC_task_index[_id].thread == _thread)
        {
          _result = _id;
        }
    }
  return _result;
}

SARC_int32
SARC_task_start (SARC_uint32 _id, void (*routine))
{
  SARC_int32 _result = SARC_FAILURE;
  pthread_t thread;
  if (_id >= SARC_MAX_NB_TASK)
    {
      return _result;
    }

  if (SARC_task_index[_id].is_started != SARC_TRUE)
    {
      pthread_create (&thread, NULL, routine, NULL);
      if (errno)
        {
          perror ("pthread_create");
          abort ();
        }
      SARC_task_index[_id].is_started = SARC_TRUE;
      SARC_task_index[_id].thread = thread;
      _result = SARC_SUCCESS;
    }
  return _result;
}
