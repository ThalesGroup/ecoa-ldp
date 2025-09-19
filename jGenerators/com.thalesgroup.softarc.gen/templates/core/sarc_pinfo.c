/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp.h"


void SARC_init_pinfo(SARC_Pinfo* pinfo, char* path, char* name)
{
  pinfo->fd = open (path, O_RDONLY, 0);
  if (pinfo->fd == -1)
    {
      SARC_log_error("Error open pinfo for %s", name);
    }
  else
    {
      /* Move the head to the end of the file */
      pinfo->size = lseek (pinfo->fd, 0, SEEK_END);
      pinfo->head = lseek (pinfo->fd, 0, SEEK_SET);
    }
}

void SARC_close_pinfo(SARC_Pinfo* pinfo)
{
	if (close(pinfo->fd) == -1)
	  {
	  	SARC_log_error("Error closing file descriptor");
	  }
}

SARC_Ecode SARC_read_pinfo(char* name, SARC_Pinfo* pinfo, void* memory_address, SARC_MwSize capacity, SARC_MwSize* out_size)
{
  SARC_Ecode _result = SARC_SUCCESS;
  ssize_t _offset = 0;
  _offset = read(pinfo->fd, memory_address, capacity);

  if (_offset == -1)
    {
      _result = SARC_FAILURE;
      SARC_log_error("Error read on %s", name);
    }
    else
    {
      *out_size = _offset;
      pinfo->head += _offset;
    }

  return _result;
}

SARC_Ecode SARC_seek_pinfo(char* name, SARC_Pinfo* pinfo, SARC_int32 offset, SARC_PInfoOrigin origin, SARC_MwSize* position)
{
  SARC_Ecode _result = SARC_SUCCESS;
  off_t _position = 0;
  off_t _real_position = 0;
  switch (origin)
    {
      case SARC_PINFO_ORIGIN_START:
        if ((SARC_uint32) offset > pinfo->size)
          {
            _result = SARC_FAILURE;
            SARC_log_error ("%s_PINFO_seek with parameter whence SARC_PINFO_ORIGIN_START", name);
          }
        else
          {
            _position = offset;
          }
        break;
      case SARC_PINFO_ORIGIN_CURRENT:
        if (((SARC_uint32) offset > pinfo->size - pinfo->head)
            || (offset <= 0 && (SARC_uint32) (-offset) > pinfo->head))
          {
            _result = SARC_FAILURE;
            SARC_log_error ("%s_PINFO_seek with parameter whence SARC_PINFO_ORIGIN_CURRENT", name);
          }
        else
          {
            _position = pinfo->head + offset;
          }
        break;
      case SARC_PINFO_ORIGIN_END:
        if ((SARC_uint32) (-offset) > pinfo->size)
          {
            _result = SARC_FAILURE;
            SARC_log_error ("%s_PINFO_seek with parameter whence SARC_PINFO_ORIGIN_END", name);
          }
        else
          {
            _position = pinfo->size - (-offset);
          }
        break;
      default:
        _result = SARC_FAILURE;
        SARC_log_error ("%s_PINFO_seek with parameter whence not available", name);
        break;
    }

  if (_result == SARC_SUCCESS)
    {
      _real_position = lseek (pinfo->fd, _position, SEEK_SET);
      if (_real_position == (off_t) (-1) || _real_position != _position)
        {
          _result = SARC_FAILURE;
          SARC_log_error ("%s_PINFO_seek on position computation", name);
        }
      else
        {
          pinfo->head = _real_position;
          *position = _real_position;
        }
    }
  return _result;
}
