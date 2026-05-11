/* Copyright © 2015 – 2025 Thales Group
 * All Rights Reserved.
 */

#include "sarc_ldp_internal.h"


void
SARC_serial_copy_or_swap_2bytes (void *dest, const void *src, SARC_boolean8 with_swap)
{
    SARC_char8 *_dest = dest;
    const SARC_char8 *_src = src;

    if (with_swap != SARC_FALSE)
      {
        _dest[0] = _src[1];
        _dest[1] = _src[0];
      }
    else
      {
        _dest[0] = _src[0];
        _dest[1] = _src[1];
      }
}

void
SARC_serial_copy_or_swap_4bytes (void *dest, const void *src, SARC_boolean8 with_swap)
{
    SARC_char8 *_dest = dest;
    const SARC_char8 *_src = src;

    if (with_swap != SARC_FALSE)
      {
        _dest[0] = _src[3];
        _dest[1] = _src[2];
        _dest[2] = _src[1];
        _dest[3] = _src[0];
      }
    else
      {
        _dest[0] = _src[0];
        _dest[1] = _src[1];
        _dest[2] = _src[2];
        _dest[3] = _src[3];
      }
}

void
SARC_serial_copy_or_swap_8bytes (void *dest, const void *src, SARC_boolean8 with_swap)
{
    SARC_char8 *_dest = dest;
    const SARC_char8 *_src = src;

    if (with_swap != SARC_FALSE)
      {
        _dest[0] = _src[7];
        _dest[1] = _src[6];
        _dest[2] = _src[5];
        _dest[3] = _src[4];
        _dest[4] = _src[3];
        _dest[5] = _src[2];
        _dest[6] = _src[1];
        _dest[7] = _src[0];
      }
    else
      {
        _dest[0] = _src[0];
        _dest[1] = _src[1];
        _dest[2] = _src[2];
        _dest[3] = _src[3];
        _dest[4] = _src[4];
        _dest[5] = _src[5];
        _dest[6] = _src[6];
        _dest[7] = _src[7];
      }
}


void
SARC_serial_start_serialize (SARC_SerializationContext * s, void *buffer, SARC_boolean8 with_swap)
{
  s->buffer = buffer;
  s->pos = 0;
  s->error = SARC_FALSE;
  s->local_error = SARC_FALSE;
  s->with_swap = with_swap;
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
  SARC_serial_copy_or_swap_2bytes (s->buffer + s->pos, src, s->with_swap);
  s->pos += 2;
}

void
SARC_serial_serialize_4bytes (SARC_SerializationContext * s, const void *src)
{
  SARC_serial_copy_or_swap_4bytes (s->buffer + s->pos, src, s->with_swap);
  s->pos += 4;
}

void
SARC_serial_serialize_8bytes (SARC_SerializationContext * s, const void *src)
{
  SARC_serial_copy_or_swap_8bytes (s->buffer + s->pos, src, s->with_swap);
  s->pos += 8;
}

void
SARC_serial_start_deserialize (SARC_DeserializationContext * s,
                               const void *buffer, SARC_MwSize size,
                               SARC_boolean8 with_swap)
{
  s->buffer = buffer;
  s->raw_size = size;
  s->pos = 0;
  s->error = SARC_FALSE;
  s->check_underflow = SARC_TRUE;
  s->with_swap = with_swap;
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
  SARC_serial_copy_or_swap_2bytes (dest, s->buffer + s->pos, s->with_swap);
  s->pos += 2;
}

void
SARC_serial_deserialize_4bytes (SARC_DeserializationContext * s, void *dest)
{
  SARC_serial_copy_or_swap_4bytes (dest, s->buffer + s->pos, s->with_swap);
  s->pos += 4;
}

void
SARC_serial_deserialize_8bytes (SARC_DeserializationContext * s, void *dest)
{
  SARC_serial_copy_or_swap_8bytes (dest, s->buffer + s->pos, s->with_swap);
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