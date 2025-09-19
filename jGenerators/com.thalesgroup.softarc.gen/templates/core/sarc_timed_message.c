/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp.h"

void
SARC_timed_message_add (SARC_timed_message ** head, const void *message,
                        SARC_uint32 size, SARC_int64 ts)
{
  SARC_timed_message *new_message =
    (SARC_timed_message *) malloc (sizeof (SARC_timed_message));
  if (size > 0)
    {
      new_message->message = malloc (size);
      memcpy (new_message->message, message, size);
    }
  else
    {
      new_message->message = NULL;
    }
  new_message->size = size;
  new_message->ts = ts;
  // find insertion point, so that messages are sorted by increasing timestamp
  while (*head != NULL && ts > (*head)->ts)
    {
      head = &(*head)->next;
    }
  // new message is before *head; or *head is NULL
  new_message->next = *head;
  *head = new_message;
}


SARC_timed_message *
SARC_timed_message_delete (SARC_timed_message ** head,
                           SARC_timed_message * message_to_delete)
{
  int found = 0;
  SARC_timed_message * previous = NULL;

  for(SARC_timed_message* m = *head; m != NULL; m = m->next) {
      if (m == message_to_delete) {
          found = 1;
          break;
      }
      previous = m;
  }

  if (found)
    {
      SARC_timed_message * next = message_to_delete->next;
      if (previous != NULL)
          previous->next = next;
      else
          *head = next;

      free (message_to_delete->message);
      free (message_to_delete);

      return next;
    }
  else
      return NULL;
}

int
SARC_timed_message_trigger_sendto (SARC_int32 oper_id, SARC_int64 timeout,
                                   const struct sockaddr_un *addr)
{
  SARC_SerializationContext s;
  SARC_int32 buffer[4];
  SARC_uint32 sarc_oper_id_timed_message = SARC_OP_TIMED_MESSAGE;

  SARC_serial_start_serialize (&s, &buffer);
  SARC_uint32_serialize (&s, &sarc_oper_id_timed_message);
  SARC_int64_serialize (&s, &timeout);
  SARC_uint32_serialize (&s, &oper_id);
  /* Send trigger setting */
  return sendto (SARC_socket_out, s.buffer, s.pos, 0,
                 (const struct sockaddr *) addr, sizeof (struct sockaddr_un));
}

SARC_int64
SARC_get_local_time ()
{
  struct timespec _time;
  clock_gettime (CLOCK_MONOTONIC, &_time);

  SARC_int64 _result = _time.tv_sec;
  _result *= 1000000000;
  _result += _time.tv_nsec;
  return _result;
}
