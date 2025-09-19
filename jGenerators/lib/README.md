# Dependencies

## JAXB (Jakarta XML Binding)

JAXB is used to generate Java code from XSD schema definitions.

* Homepage: https://eclipse-ee4j.github.io/jaxb-ri
* Version: 4.0.1
* Documentation: https://eclipse-ee4j.github.io/jaxb-ri/4.0.1/docs/
* Origin URL: https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-ri/4.0.1/jaxb-ri-4.0.1.zip


## StringTemplate

StringTemplate is used by the ECOA-LDP for code generation.

* Homepage : https://www.stringtemplate.org/
* Version : 4.0.1
* Origin URL : https://github.com/antlr/website-st4/blob/gh-pages/download/ST-4.0.1.jar

**Warning**: The file shall be renamed to `stringtemplate-4.0.1.jar`.


## Antlr

Antlr is used by StringTemplate.

* Homepage : http://www.antlr3.org/
* Version : 3.3
* Origin URL : https://repo1.maven.org/maven2/org/antlr/antlr-runtime/3.3/antlr-runtime-3.3.jar

**Warning**: The file shall be renamed to `antlr-3.3-runtime.jar`.


# Instructions to download and install dependencies

Here is a list of shell commands that should install all the needed Java dependencies,
and install them in the expected directories.

Adapt the script if needed.

```bash
cd $SOFTARC_HOME/jGenerators/lib

mkdir jaxb-ri-4.0.1
wget https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-ri/4.0.1/jaxb-ri-4.0.1.zip
unzip jaxb-ri-4.0.1.zip jaxb-ri/mod/*.jar -d jaxb-ri-4.0.1
mv jaxb-ri-4.0.1/jaxb-ri/mod/*.jar jaxb-ri-4.0.1
mkdir jaxb-ri-4.0.1/devel
mv jaxb-ri-4.0.1/jaxb-xjc.jar jaxb-ri-4.0.1/devel

mkdir StringTemplate
wget https://github.com/antlr/website-st4/raw/refs/heads/gh-pages/download/ST-4.0.1.jar
mv ST-4.0.1.jar StringTemplate/stringtemplate-4.0.1.jar

mkdir antlr3
wget https://repo1.maven.org/maven2/org/antlr/antlr-runtime/3.3/antlr-runtime-3.3.jar
mv antlr-runtime-3.3.jar antlr3/antlr-3.3-runtime.jar
```
