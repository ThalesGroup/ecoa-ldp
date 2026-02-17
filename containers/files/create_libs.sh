#!/usr/bin/env bash
# setup all needed libraries
# we also install (apart from ecoa)
# - a json c lib
# - protobuf c-libs (compiler, code generator)
# - protobuf python lib
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
function python_modules() {
	pip -q install grpcio-tools
	apt install python3-grpcio
}
function local_protobuf_library() {
  mkdir -p TMP
  pushd TMP
	git clone https://github.com/protocolbuffers/protobuf.git
	pushd protobuf
	git checkout v26.1
	git submodule update --init
	mkdir -p build
	pushd build
	cmake -DCMAKE_INSTALL_PREFIX=/usr/local ..
	make -j6 all install
	popd
	popd
	popd
}
function json_header() {
  mkdir -p TMP
  pushd TMP
	git clone https://github.com/sheredom/json.h.git
	install -d -m 755 /usr/local/include/sheredom
	install -D -m 664 --target-directory=/usr/local/include/sheredom json.h/json.h 
	popd
}
function local_protobuf_compiler() {
  mkdir -p TMP
  pushd TMP
	git clone ${PROTOC_REPO} protobuf-c
	git checkout v1.0.2
	pushd protobuf-c
	patch -p1 -- << END
diff --git a/protoc-gen-c/c_generator.h b/protoc-gen-c/c_generator.h
index c19a786..5fd34c8 100644
--- a/protoc-gen-c/c_generator.h
+++ b/protoc-gen-c/c_generator.h
@@ -94,7 +94,7 @@ class PROTOBUF_C_EXPORT CGenerator : public google::protobuf::compiler::CodeGene
                 std::string* error) const;
 
 #if GOOGLE_PROTOBUF_VERSION >= 5026000
-  uint64_t GetSupportedFeatures() const { return 0; }
+  uint64_t GetSupportedFeatures() const { return FEATURE_PROTO3_OPTIONAL; }
   google::protobuf::Edition GetMinimumEdition() const { return google::protobuf::Edition::EDITION_PROTO2; }
   google::protobuf::Edition GetMaximumEdition() const { return google::protobuf::Edition::EDITION_PROTO3; }
 #endif
END
	./autogen.sh
	./configure
	make -j4 all
	make install
	popd
	popd
}
rm -rf "$LDP_DIR"
rm -rf TMP
git clone "$REPO" "$LDP_DIR"
mkdir -p "$LIB_DIR"

git config --global --add safe.directory /ecoa

thales_stack
python_modules
local_protobuf_library
local_protobuf_compiler
json_header

cd $LDP_DIR
git checkout HEAD
ant clean build
