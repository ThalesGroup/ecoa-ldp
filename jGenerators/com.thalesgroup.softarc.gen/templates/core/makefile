# Copyright (c) 2025 THALES -- All rights reserved

# Configuration ----------------------------------------

EXE = a.out

LD = gcc
CC = gcc
AR = ar

CFLAGS   = -std=gnu99 -ggdb -W -Wall -Wno-unused-parameter -Wextra -Wformat -DSARC_IS_LDP

LDFLAGS  = "-lrt" "-lpthread" -lm

INCDIRS = ../../01-Components/*/*/inc* ../../00-Types/*/*/inc-gen
SRCDIRS = ../../01-Components/*/*/src*/*.c ../../00-Types/*/*/src-gen/*.c

-include makefile_dirs
-include defs.mak

all: $(EXE)

# Compilation ------------------------------------------

CFLAGS += -I"inc-gen/"
CFLAGS += $(patsubst %,-I%,$(wildcard $(INCDIRS)))

cflags:
	echo $(CFLAGS) > $@

COMPILE.c = $(CC) @cflags $(TARGET_ARCH) -c

MAIN_SRC = $(wildcard src-gen/*.c src-gen/*/*.c)
CMP_SRC = $(wildcard $(SRCDIRS))

# Link ------------------------------------------

MAIN_OBJECTS = $(patsubst %.c,%.o,$(MAIN_SRC))
CMP_OBJECTS =  $(patsubst %.c,%.o,$(CMP_SRC))

libCOMP.a: $(CMP_OBJECTS)
	$(AR) r $@ $^

$(EXE): cflags $(MAIN_OBJECTS) libCOMP.a
	@echo ". Linking $@"
	$(LD) -o "$@" $(LDFLAGS) $(MAIN_OBJECTS) libCOMP.a
	ls -l $@

#-include obj/*.d

clean:
	rm -f cflags $(MAIN_OBJECTS) $(CMP_OBJECTS) libCOMP.a $(EXE)
