#!/usr/bin/env bash

TOP=`git rev-parse --show-toplevel`
export TOP

# <dockerfile> <image label> <docker directory>

function build_container() {
	echo "- building"
	cat "$2" \
		| docker buildx build \
		--progress=plain \
		--build-arg HTTP_PROXY=$http_proxy \
		--build-arg HTTPS_PROXY=$https_proxy \
    --build-arg NO_PROXY=$no_proxy \
		-t "$1" -f - .
}

build_container ecoa-as7-zenoh-devel Dockerfile
