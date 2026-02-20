#!/usr/bin/env bash
# setup all needed libs, paths, directories
export LDP_DIR=/ecoa/ldp
export LIB_DIR=$LDP_DIR/jGenerators/lib
export REPO="https://github.com/ThalesGroup/ecoa-ldp"
export PROTOC_REPO="https://github.com/protobuf-c/protobuf-c.git"
export SOFTARC_HOME=/ecoa/ldp
export PATH=/ecoa/apache-ant-1.10.8/bin:$PATH
export LDP_DIR="/ecoa/ldp"
export LIB_DIR=$LDP_DIR/jGenerators/lib
export CMAKE_INSTALL_PREFIX=/usr/local

function thales_stack() {
	mkdir -p TMP
	pushd TMP

	wget https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-ri/4.0.1/jaxb-ri-4.0.1.zip
	unzip jaxb-ri-4.0.1.zip jaxb-ri/mod/*.jar -d $LIB_DIR/jaxb-ri-4.0.1
	mv $LIB_DIR/jaxb-ri-4.0.1/jaxb-ri/mod/*.jar $LIB_DIR/jaxb-ri-4.0.1
	mkdir $LIB_DIR/jaxb-ri-4.0.1/devel
	mv $LIB_DIR/jaxb-ri-4.0.1/jaxb-xjc.jar $LIB_DIR/jaxb-ri-4.0.1/devel

	mkdir $LIB_DIR/StringTemplate
	wget https://github.com/antlr/website-st4/raw/refs/heads/gh-pages/download/ST-4.0.1.jar
	mv ST-4.0.1.jar $LIB_DIR/StringTemplate/stringtemplate-4.0.1.jar

	mkdir $LIB_DIR/antlr3
	wget https://repo1.maven.org/maven2/org/antlr/antlr-runtime/3.3/antlr-runtime-3.3.jar
	mv antlr-runtime-3.3.jar $LIB_DIR/antlr3/antlr-3.3-runtime.jar

	wget https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.8-bin.zip
	unzip apache-ant-1.10.8-bin.zip
	mv apache-ant-1.10.8 /ecoa

	popd
}

rm -rf "$LDP_DIR"
rm -rf TMP
git clone "$REPO" "$LDP_DIR"
mkdir -p "$LIB_DIR"

git config --global --add safe.directory /ecoa

thales_stack

cd $LDP_DIR
git checkout HEAD
ant clean build
