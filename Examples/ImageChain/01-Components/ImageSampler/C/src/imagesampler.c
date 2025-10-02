#include "imagesampler.h"


void imagesampler_initialize(imagesampler_context *ctxt)
{
  /* TODO: to be implemented */
}
void imagesampler_start(imagesampler_context *ctxt)
{
  /* TODO: to be implemented */
}
void imagesampler_stop(imagesampler_context *ctxt)
{
  /* TODO: to be implemented */
}
void imagesampler_shutdown(imagesampler_context *ctxt)
{
  /* TODO: to be implemented */
}
void imagesampler_reset(imagesampler_context *ctxt)
{
  /* TODO: to be implemented */
}


void imagesampler_tick_EVENT_receive(imagesampler_context *ctxt
)
{
  image_ImageType image;

  image_ImageType_initialize (&image);

  imagesampler_image_out_EVENT_send (ctxt, &image);
}
