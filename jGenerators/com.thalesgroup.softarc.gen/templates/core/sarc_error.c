/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp.h"

void
SARC_log_info (const char *sarc_format, ...)
{
  va_list sarc_args;

  printf ("INFO : ");
  va_start (sarc_args, sarc_format);
  vprintf (sarc_format, sarc_args);
  va_end (sarc_args);
  putc ('\n', stdout);
  fflush (stdout);
}

void
SARC_log_warning (const char *sarc_format, ...)
{
  va_list sarc_args;

  printf ("WARNING : ");
  va_start (sarc_args, sarc_format);
  vprintf (sarc_format, sarc_args);
  va_end (sarc_args);
  putc ('\n', stdout);
  fflush (stdout);
}

void
SARC_log_error (const char *sarc_format, ...)
{
  va_list sarc_args;

  fprintf (stderr, "ERROR: ");
  va_start (sarc_args, sarc_format);
  vfprintf (stderr, sarc_format, sarc_args);
  va_end (sarc_args);
  putc ('\n', stderr);
  fflush (stderr);
}

void
SARC_error_raise (SARC_ErrorCode code, SARC_int32 i1,
                  SARC_int32 i2, const SARC_char8 * s1)
{
  if (code == SARC_ERROR_FIFOSIZE_OVERFLOW)
    {
      fprintf (stderr,
               "ERROR: possible fifoSize overflow when sending operation '%s.%s' to task '%s'\n",
               SARC_table_name_of_instance[i1], s1,
               SARC_table_name_of_task[i2]);
      return;
    }
  SARC_log_error (s1);
}

void
SARC_trace_error (SARC_TraceLevel level, SARC_uint32 id,
                  const SARC_char8 * message)
{
  SARC_log_error (message);
}
