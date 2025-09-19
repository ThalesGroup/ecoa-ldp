/* Copyright © 2015 – 2025 Thales Group
 * All Rights Reserved.
 */

#include "sarc_ldp_internal.h"


void
SARC_serial_copy_or_swap_2bytes (void *dest, const void *src)
{
  memcpy (dest, src, 2);
}

void
SARC_serial_copy_or_swap_4bytes (void *dest, const void *src)
{
  memcpy (dest, src, 4);
}

void
SARC_serial_copy_or_swap_8bytes (void *dest, const void *src)
{
  memcpy (dest, src, 8);
}


void
SARC_serial_start_serialize (SARC_SerializationContext * s, void *buffer)
{
  s->buffer = buffer;
  s->pos = 0;
  s->error = SARC_FALSE;
  s->local_error = SARC_FALSE;
}

void
SARC_serial_check_serialize (SARC_SerializationContext * s,
                             const SARC_char8 * name)
{
  if (s->local_error == SARC_TRUE)
    {
      s->error = SARC_TRUE;
      s->local_error = SARC_FALSE;

      SARC_log_error ("error in serialization of %s", name);
    }
}

void
SARC_serial_serialize_1byte (SARC_SerializationContext * s, const void *src)
{
  s->buffer[s->pos] = *(const SARC_char8 *) src;
  s->pos += 1;
}

void
SARC_serial_serialize_2bytes (SARC_SerializationContext * s, const void *src)
{
  SARC_serial_copy_or_swap_2bytes (s->buffer + s->pos, src);
  s->pos += 2;
}

void
SARC_serial_serialize_4bytes (SARC_SerializationContext * s, const void *src)
{
  SARC_serial_copy_or_swap_4bytes (s->buffer + s->pos, src);
  s->pos += 4;
}

void
SARC_serial_serialize_8bytes (SARC_SerializationContext * s, const void *src)
{
  SARC_serial_copy_or_swap_8bytes (s->buffer + s->pos, src);
  s->pos += 8;
}

void
SARC_serial_start_deserialize (SARC_DeserializationContext * s,
                               const void *buffer, SARC_MwSize size)
{
  s->buffer = buffer;
  s->raw_size = size;
  s->pos = 0;
  s->error = SARC_FALSE;
  s->check_underflow = SARC_TRUE;
}

void
SARC_serial_check_deserialize (SARC_DeserializationContext * s,
                               const SARC_char8 * name)
{
  if (s->raw_size > 0)
    {
      if (s->pos > s->raw_size)
        {
          /* overflow */
          s->error = SARC_TRUE;
          SARC_log_error
            ("too many bytes in deserialization of %s (expected %d, got %d)",
             name, s->raw_size, s->pos);
        }
      else if (s->pos < s->raw_size && s->check_underflow != SARC_FALSE)
        {
          /* underflow */
          SARC_log_error
            ("too few bytes in deserialization of %s (expected %d, got %d)",
             name, s->raw_size, s->pos);
        }
    }
}

void
SARC_serial_deserialize_1byte (SARC_DeserializationContext * s, void *dest)
{
  *(SARC_char8 *) dest = s->buffer[s->pos];
  s->pos += 1;
}

void
SARC_serial_deserialize_2bytes (SARC_DeserializationContext * s, void *dest)
{
  SARC_serial_copy_or_swap_2bytes (dest, s->buffer + s->pos);
  s->pos += 2;
}

void
SARC_serial_deserialize_4bytes (SARC_DeserializationContext * s, void *dest)
{
  SARC_serial_copy_or_swap_4bytes (dest, s->buffer + s->pos);
  s->pos += 4;
}

void
SARC_serial_deserialize_8bytes (SARC_DeserializationContext * s, void *dest)
{
  SARC_serial_copy_or_swap_8bytes (dest, s->buffer + s->pos);
  s->pos += 8;
}

SARC_boolean8
SARC_boolean8_check (const SARC_boolean8 * value, SARC_char8 * msg)
{
  SARC_boolean8 _result = SARC_TRUE;
  const SARC_uint8 *const _value = (const SARC_uint8 *) value;

  if (*_value > 1)
    {
      _result = SARC_FALSE;
      strcpy (msg, "value is not allowed in type SARC_boolean8");
    }

  return _result;
}
