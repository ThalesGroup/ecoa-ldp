#include "HMI.h"

/* TODO: to be implemented */


void HMI_initialize(HMI_context *ctxt)
{
  /* TODO: to be implemented */
}
void HMI_start(HMI_context *ctxt)
{
  /* TODO: to be implemented */
}
void HMI_stop(HMI_context *ctxt)
{
  /* TODO: to be implemented */
}
void HMI_shutdown(HMI_context *ctxt)
{
  /* TODO: to be implemented */
}
void HMI_reset(HMI_context *ctxt)
{
  /* TODO: to be implemented */
}


void HMI_image_in_EVENT_receive(HMI_context *ctxt,
  const image_ImageType *image
)
{
  HMI_trace(ctxt, SARC_TRACE_INFO, "received an image"); 
}
