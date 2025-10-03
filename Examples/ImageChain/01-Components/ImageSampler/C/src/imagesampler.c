/*
 * @file imagesampler.c
 * Module Interface for Module imagesampler
 */

/* Include module interface header */
#include "imagesampler.h"
#include <string.h>

/* Event operation handlers */
void 
imagesampler__tick__received (
  imagesampler__context* context
 )
{
  image__ImageType image;

  memset (&image, 0, sizeof(image));

  imagesampler_container__image_out__send (context, &image);
}



/* Request-Response operation handlers */

/* Lifecycle operation handlers */

void imagesampler__INITIALIZE__received(imagesampler__context* context)
{
  /* TODO: to be implemented */
}

void imagesampler__START__received(imagesampler_context* context)
{
  /* TODO: to be implemented */
}

void imagesampler__STOP__received(imagesampler_context* context)
{
  /* TODO: to be implemented */
}

void imagesampler__SHUTDOWN__received(imagesampler_context* context)
{
  /* TODO: to be implemented */
}


 