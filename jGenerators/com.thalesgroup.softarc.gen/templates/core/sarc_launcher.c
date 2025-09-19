/* Copyright (c) 2025 THALES -- All rights reserved */

#include "sarc_ldp.h"

#include <sys/wait.h>

void
SARC_launcher ()
{
  char linec[512];
  int linenumber = 0;
  int tty = isatty (0);

  if (tty)
    {
      puts ("List of available commands:");
      puts ("  Wait <duration>     : wait for <duration> milliseconds");
      puts ("  Init     <component>");
      puts ("  Start    <component>");
      puts ("  Reset    <component>");
      puts ("  Stop     <component>");
      puts ("  Shutdown <component>");
      puts ("  Quit");
      puts ("");
      puts ("<component> is the instance id, or '*' for all instances.");
      puts ("<component> may be omitted, meaning all instances.");
      puts ("Lines starting with '#' are ignored.");
      puts ("");
    }
  while (1)
    {
      char optype[12] = "";
      char secondattr[128] = "";
      int duration;
      char durationunit[4] = "";
      SARC_LifeCycleShift transition = SARC_LIFE_CYCLE_SHIFT_NONE;

      usleep (100000);
      if (tty)
        {
          printf (">> ");
          fflush (stdout);
        }
      if (fgets (linec, 512, stdin) == NULL)
        break;
      if (!tty)
        puts (linec);
      linenumber += 1;
      int argc = sscanf (linec, "%12s", optype);
      if (argc == 0 || optype[0] == '\0' || optype[0] == '#')
        {
          continue;
        }
      if (strcasecmp (optype, "Quit") == 0)
        {
          return;
        }
      else if (strcasecmp (optype, "Wait") == 0)
        {
          argc =
            sscanf (linec, "%12s %i %3s", optype, &duration, durationunit);
          if (argc >= 2)
            {
              if (argc == 3)
                {
                  if (strcmp (durationunit, "ms") == 0)
                    {
                    }
                  else if (strcmp (durationunit, "s") == 0)
                    {
                      duration *= 1000;
                    }
                  else
                    {
                      printf ("Unknown time unit: %s\n", durationunit);
                    }
                }
              printf ("Waiting for %d ms ...\n", duration);
              fflush (stdout);
              usleep (duration * 1000);
              printf ("End of wait\n");
            }
        }
      else
        {
          argc = sscanf (linec, "%12s %128s", optype, secondattr);
          if (strcasecmp (optype, "Init") == 0)
            transition = SARC_LIFE_CYCLE_SHIFT_INITIALIZE;
          else if (strcasecmp (optype, "Start") == 0)
            transition = SARC_LIFE_CYCLE_SHIFT_START;
          else if (strcasecmp (optype, "Reset") == 0)
            transition = SARC_LIFE_CYCLE_SHIFT_RESET;
          else if (strcasecmp (optype, "Stop") == 0)
            transition = SARC_LIFE_CYCLE_SHIFT_STOP;
          else if (strcasecmp (optype, "Shutdown") == 0)
            transition = SARC_LIFE_CYCLE_SHIFT_SHUTDOWN;
          if (transition != SARC_LIFE_CYCLE_SHIFT_NONE)
            {
              if (argc == 1 || secondattr[0] == '\0'
                  || strcasecmp (secondattr, "*") == 0)
                {
                  for (int i = 0; i < SARC_MAX_NB_COMPONENT; i++)
                    {
                      SARC_gen_send_lifecycle_command (i, transition);
                      usleep (10000);
                    }
                }
              else
                {
                  int i;
                  if (sscanf (secondattr, "%d", &i) < 1 || i < 0
                      || i >= SARC_MAX_NB_COMPONENT)
                    printf ("Invalid instance id: %s\n", secondattr);
                  else
                    SARC_gen_send_lifecycle_command (i, transition);
                }
            }
          else
            {
              printf ("Unknown command: %s\n", linec);
            }
        }
    }

  // end of file: exit if input is a terminal, or else wait forever
  if (!tty)
    while (1)
      wait (NULL);
}
