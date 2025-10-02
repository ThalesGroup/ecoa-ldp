#include "detector.h"

/* TODO: to be implemented */


void detector_initialize(detector_context *ctxt)
{
  /* TODO: to be implemented */
}
void detector_start(detector_context *ctxt)
{
  /* TODO: to be implemented */
}
void detector_stop(detector_context *ctxt)
{
  /* TODO: to be implemented */
}
void detector_shutdown(detector_context *ctxt)
{
  /* TODO: to be implemented */
}
void detector_reset(detector_context *ctxt)
{
  /* TODO: to be implemented */
}


void detector_image_in_EVENT_receive(detector_context *ctxt,
  const image_ImageType *image
)
{
  detector_image_out_EVENT_send (ctxt, image);
}


/* TODO: to be implemented */
