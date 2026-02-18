#!/bin/bash

if [ $# -lt 1 ]; then
	echo -e "* Please specify container-to-run and workdir to mount\n"
	echo -e "Usage: $0 <workdir-to-mount>\n"
else

TMPDIR=$(mktemp -d)

docker run -tid --rm \
    -u $(id -u)\
    -e http_proxy=${http_proxy} \
    -e https_proxy=${https_proxy} \
    -e _USER=$USER \
    -e _UID=$(id -u $USER) -e _GID=$(id -g $USER) \
    -e MYTEMP=$TMPDIR \
    --net=host \
    -v /tmp:/tmp \
    -v $HOME/.ssh:/ecoa/.ssh \
    -v $TMPDIR:/ecoa/tmp \
    -v $1:/ecoa/WORK \
    --name ${USER}-$(uuidgen) ecoa-as7-zenoh-devel
    # do not run the command automatically.
    # attach to the container and run in manually
    # you can the export the created exported data with ssh/git
fi
