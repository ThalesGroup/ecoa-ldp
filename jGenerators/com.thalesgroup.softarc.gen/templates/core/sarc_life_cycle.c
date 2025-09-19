/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp_internal.h"


const char *SARC_table_name_of_life_cycle_state[4] = {
  "UNAVAILABLE",
  "IDLE",
  "READY",
  "RUNNING",
};

const char *SARC_table_name_of_life_cycle_shift[8] = {
  "shift0-undefined",
  "RISE",
  "INITIALIZE",
  "START",
  "RESET",
  "STOP",
  "SHUTDOWN",
  "KILL",
};

SARC_LifeCycleStateStatus
SARC_life_cycle_next_state (SARC_LifeCycleState state,
                            SARC_LifeCycleShift shift)
{
  SARC_LifeCycleStateStatus _result = { state, SARC_FALSE };

  switch (shift)
    {
    case SARC_LIFE_CYCLE_SHIFT_RISE:
      if (state == SARC_LIFE_CYCLE_STATE_UNAVAILABLE)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_IDLE;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_INITIALIZE:
      if (state == SARC_LIFE_CYCLE_STATE_IDLE)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_READY;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_START:
      if (state == SARC_LIFE_CYCLE_STATE_READY)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_RUNNING;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_RESET:
      if (state == SARC_LIFE_CYCLE_STATE_RUNNING)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_RUNNING;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_STOP:
      if (state == SARC_LIFE_CYCLE_STATE_RUNNING)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_READY;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_SHUTDOWN:
      if (state != SARC_LIFE_CYCLE_STATE_UNAVAILABLE)
        {
          _result.state = SARC_LIFE_CYCLE_STATE_IDLE;
          _result.is_state_changed = SARC_TRUE;
        }
      break;

    case SARC_LIFE_CYCLE_SHIFT_KILL:
      _result.state = SARC_LIFE_CYCLE_STATE_UNAVAILABLE;
      _result.is_state_changed = SARC_TRUE;
      break;

    default:
      break;
    }

  return _result;
}
