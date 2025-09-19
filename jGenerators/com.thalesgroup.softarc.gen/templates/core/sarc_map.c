/* Copyright © 2015 – 2024 Thales Group
 * All Rights Reserved.
 */
#include "sarc_ldp_internal.h"


void *SARC_map_set (void *address, SARC_int64 key);

SARC_boolean8 SARC_map_has (const void *address, SARC_int64 key,
                            SARC_MwSize * position);

/**
 * Context for associative container management.
 */
typedef struct
{
  /** Maximum size, in bytes, occupied by each value */
  SARC_MwSize value_size;
  /** Offset of the internal indexed list */
  SARC_MwSize list_offset;
} SARC_MapHeader;

/**
 * Context for indexed list management.
 */
typedef struct
{
  /** Number of items used in the list */
  SARC_MwSize count;
  /** Maximum number of items available in the list */
  SARC_MwSize capacity;
  /** Size, in bytes, reserved for each item */
  SARC_MwSize item_size;
  /** Offset of the logical index */
  SARC_MwSize index_offset;
  /** Offset of the items storage space */
  SARC_MwSize store_offset;
} SARC_TableHeader;


/* ===========================================================================
 * Internal helper function
 * ======================================================================== */

SARC_Ecode
SARC_table_initialize (void *address, SARC_MwSize size, SARC_MwSize capacity,
                       SARC_MwSize item_size)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_TableHeader *_header = NULL;
  SARC_MwSize _store_offset = 0;
  SARC_MwSize _total_size = 0;
  SARC_MwSize *_index = NULL;
  SARC_MwSize _offset = 0;
  SARC_MwSize _i = 0;

  /* Determine minimum size of the buffer required to manage such amount of
     items */

  _store_offset = sizeof (SARC_TableHeader) + capacity * sizeof (SARC_MwSize);
  _store_offset += 7;
  _store_offset = _store_offset & (_store_offset ^ 7);

  _total_size = _store_offset + item_size * capacity;

  if (size < _total_size)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      memset (address, 0, size);

      _header = (SARC_TableHeader *) address;
      _header->count = 0;
      _header->capacity = capacity;
      _header->item_size = item_size;
      _header->index_offset = sizeof (SARC_TableHeader);
      _header->store_offset = _store_offset;

      _index =
        (SARC_MwSize *) ((SARC_Byte *) address + _header->index_offset);
      _offset = _header->store_offset;
      for (_i = 0; _i < _header->capacity; ++_i)
        {
          _index[_i] = _offset;
          _offset += _header->item_size;
        }
    }

  return _result;
}


SARC_MwSize
SARC_table_count (const void *address)
{
  SARC_MwSize _result = 0;
  const SARC_TableHeader *_header = (const SARC_TableHeader *) address;

  _result = _header->count;

  return _result;
}


SARC_Ecode
SARC_table_clear (void *address)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_TableHeader *const _header = (SARC_TableHeader *) address;

  _header->count = 0;
  return _result;
}


void *
SARC_table_get (const void *address, SARC_MwSize position)
{
  void *_result = NULL;
  const SARC_TableHeader *const _header = (const SARC_TableHeader *) address;
  const SARC_MwSize *const _index =
    (const SARC_MwSize *) ((const SARC_Byte *) address +
                           _header->index_offset);
  SARC_MwSize _offset = 0;

  if (position < _header->count)
    {
      _offset = _index[position];
      _result = (SARC_Byte *) address + _offset;
    }

  return _result;
}


SARC_Ecode
SARC_table_push (void *address, const void *item)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_TableHeader *const _header = (SARC_TableHeader *) address;
  const SARC_MwSize *const _index =
    (const SARC_MwSize *) ((SARC_Byte *) address + _header->index_offset);
  SARC_MwSize _offset = 0;
  void *_item = NULL;

  if (_header->count == _header->capacity)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      _offset = _index[_header->count];
      _item = (SARC_Byte *) address + _offset;
      memcpy (_item, item, _header->item_size);
      _header->count += 1;
    }

  return _result;
}


SARC_Ecode
SARC_table_pop (void *address, void *item)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_TableHeader *const _header = (SARC_TableHeader *) address;
  const SARC_MwSize *const _index =
    (const SARC_MwSize *) ((SARC_Byte *) address + _header->index_offset);
  SARC_MwSize _offset = 0;
  const void *_item = NULL;

  if (_header->count == 0)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      _header->count -= 1;
      if (item != NULL)
        {
          _offset = _index[_header->count];
          _item = (SARC_Byte *) address + _offset;
          memcpy (item, _item, _header->item_size);
        }
    }

  return _result;
}


SARC_Ecode
SARC_table_move (void *address, SARC_MwSize from, SARC_MwSize to)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_TableHeader *_header = (SARC_TableHeader *) address;
  SARC_MwSize *_index =
    (SARC_MwSize *) ((SARC_Byte *) address + _header->index_offset);
  SARC_MwSize _offset = 0;
  SARC_MwSize _i = 0;

  if (from >= _header->count)
    {
      _result = SARC_FAILURE;
    }
  else if (to >= _header->count)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      _offset = _index[from];
      if (from < to)
        {
          for (_i = from; _i < to; ++_i)
            {
              _index[_i] = _index[_i + 1];
            }
        }
      else
        {
          for (_i = from; _i > to; --_i)
            {
              _index[_i] = _index[_i - 1];
            }
        }
      _index[to] = _offset;
    }

  return _result;
}


SARC_Ecode
SARC_table_insert (void *address, SARC_MwSize position, const void *item)
{
  SARC_Ecode _result = SARC_SUCCESS;
  const SARC_TableHeader *const _header = (const SARC_TableHeader *) address;
  const SARC_MwSize _index = _header->count;

  if (_header->count == _header->capacity)
    {
      _result = SARC_FAILURE;
    }
  else if (position > _header->count)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      SARC_table_push (address, item);
      SARC_table_move (address, _index, position);
    }

  return _result;
}


SARC_Ecode
SARC_table_remove (void *address, SARC_MwSize position, void *item)
{
  SARC_Ecode _result = SARC_SUCCESS;
  const SARC_TableHeader *const _header = (const SARC_TableHeader *) address;

  if (position >= _header->count)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      SARC_table_move (address, position, _header->count - 1);
      SARC_table_pop (address, item);
    }

  return _result;
}


void *
SARC_table_reserve (void *address)
{
  void *_result = NULL;
  SARC_TableHeader *const _header = (SARC_TableHeader *) address;
  const SARC_MwSize *const _index =
    (const SARC_MwSize *) ((SARC_Byte *) address + _header->index_offset);
  SARC_MwSize _offset = 0;

  if (_header->count < _header->capacity)
    {
      _offset = _index[_header->count];
      _result = (SARC_Byte *) address + _offset;
      _header->count += 1;
    }

  return _result;
}


/* ===========================================================================
 * Map functions
 * ======================================================================== */

SARC_Ecode
SARC_map_initialize (void *address, SARC_MwSize size, SARC_MwSize capacity,
                     SARC_MwSize value_size)
{
  SARC_Ecode _result = SARC_SUCCESS;
  SARC_MapHeader *_header = NULL;
  void *_list = NULL;
  const SARC_MwSize _item_size =
    sizeof (SARC_int64) + 8 * ((value_size + 7) / 8);

  if (size < sizeof (SARC_MapHeader))
    {
      _result = SARC_FAILURE;
    }
  else
    {
      _header = (SARC_MapHeader *) address;
      _header->value_size = value_size;
      _header->list_offset = sizeof (SARC_MapHeader);
      _list = (SARC_Byte *) address + _header->list_offset;
      _result = SARC_table_initialize (_list,
                                       size - sizeof (SARC_MapHeader),
                                       capacity, _item_size);
    }

  return _result;
}


SARC_Ecode
SARC_map_add (void *address, SARC_int64 key, const void *value)
{
  SARC_Ecode _result = SARC_SUCCESS;
  const SARC_MapHeader *_header = (SARC_MapHeader *) address;
  SARC_boolean8 _status = SARC_TRUE;
  SARC_MwSize _index = 0;
  void *_item = NULL;

  _status = SARC_map_has (address, key, &_index);
  if (_status == SARC_FALSE)
    {
      _item = SARC_map_set (address, key);
    }

  if (_item == NULL)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      memcpy (_item, value, _header->value_size);
    }

  return _result;
}


void *
SARC_map_set (void *address, SARC_int64 key)
{
  void *_result = NULL;
  const SARC_MapHeader *_header = (SARC_MapHeader *) address;
  void *_list = (SARC_Byte *) address + _header->list_offset;
  SARC_boolean8 _status = SARC_TRUE;
  SARC_MwSize _index = 0;
  void *_item = NULL;

  _status = SARC_map_has (address, key, &_index);
  if (_status != SARC_FALSE)
    {
      _item = SARC_table_get (_list, _index);
      if (_item != NULL)
        {
          _result = (SARC_Byte *) _item + sizeof (SARC_int64);
        }
    }
  else
    {
      _item = SARC_table_reserve (_list);
      if (_item != NULL)
        {
          memcpy (_item, &key, sizeof (SARC_int64));
          _result = (SARC_Byte *) _item + sizeof (SARC_int64);
        }
    }

  return _result;
}


void *
SARC_map_get (const void *address, SARC_int64 key)
{
  void *_result = NULL;
  const SARC_MapHeader *_header = (const SARC_MapHeader *) address;
  const void *_list = (const SARC_Byte *) address + _header->list_offset;
  SARC_boolean8 _status = SARC_TRUE;
  SARC_MwSize _index = 0;
  void *_item = NULL;

  _status = SARC_map_has (address, key, &_index);
  if (_status != SARC_FALSE)
    {
      _item = SARC_table_get (_list, _index);
      _result = (SARC_Byte *) _item + sizeof (SARC_int64);
    }

  return _result;
}


SARC_Ecode
SARC_map_remove (void *address, SARC_int64 key, void *value)
{
  SARC_Ecode _result = SARC_SUCCESS;
  const SARC_MapHeader *_header = (SARC_MapHeader *) address;
  void *_list = (SARC_Byte *) address + _header->list_offset;
  SARC_boolean8 _status = SARC_TRUE;
  SARC_MwSize _index = 0;
  const void *_item = NULL;
  const void *_value = NULL;

  _status = SARC_map_has (address, key, &_index);
  if (_status == SARC_FALSE)
    {
      _result = SARC_FAILURE;
    }
  else
    {
      if (value != NULL)
        {
          _item = SARC_table_get (_list, _index);
          _value = (const SARC_Byte *) _item + sizeof (SARC_int64);
          memcpy (value, _value, _header->value_size);
        }
      SARC_table_remove (_list, _index, NULL);
    }

  return _result;
}


SARC_MwSize
SARC_map_count (const void *address)
{
  const SARC_MapHeader *_header = (const SARC_MapHeader *) address;
  const void *_list = (const SARC_Byte *) address + _header->list_offset;
  const SARC_MwSize _result = SARC_table_count (_list);

  return _result;
}


SARC_boolean8
SARC_map_has (const void *address, SARC_int64 key, SARC_MwSize * position)
{
  SARC_boolean8 _result = SARC_FALSE;
  const SARC_MapHeader *_header = (const SARC_MapHeader *) address;
  const void *_list = (const SARC_Byte *) address + _header->list_offset;
  const SARC_MwSize _count = SARC_table_count (_list);
  const SARC_int64 *_item = NULL;
  SARC_MwSize _i = 0;
  SARC_int32 _status = 0;

  while (_i < _count && _result == SARC_FALSE)
    {
      _item = SARC_table_get (_list, _i);
      _status = memcmp (_item, &key, sizeof (SARC_int64));
      if (_status == 0)
        {
          _result = SARC_TRUE;
          if (position != NULL)
            {
              *position = _i;
            }
        }
      _i += 1;
    }

  return _result;
}


SARC_Ecode
SARC_map_get_key (const void *address, SARC_MwSize position, SARC_int64 * key)
{
  SARC_Ecode _result = SARC_FAILURE;
  const SARC_MapHeader *_header = (const SARC_MapHeader *) address;
  const void *_list = (const SARC_Byte *) address + _header->list_offset;
  const void *_item = NULL;

  _item = SARC_table_get (_list, position);
  if (_item != NULL)
    {
      _result = SARC_SUCCESS;
      memcpy (key, _item, sizeof (SARC_int64));
    }

  return _result;
}


void *
SARC_map_get_value (const void *address, SARC_MwSize position)
{
  void *_result = NULL;
  const SARC_MapHeader *_header = (const SARC_MapHeader *) address;
  const void *_list = (const SARC_Byte *) address + _header->list_offset;
  void *_item = NULL;

  _item = SARC_table_get (_list, position);

  if (_item != NULL)
    {
      _result = (SARC_Byte *) _item + sizeof (SARC_int64);
    }

  return _result;
}
