# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.

MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR))

include $(MYDIR)/common/makefiles/paths.mk

PROJECT_NAME := $(notdir $(TOP))
PROJECT_NAME_LOWERCASE := $(shell echo $(PROJECT_NAME) | tr A-Z a-z)
EXTRACT_VERSION := $(TOP)/common/xslt/extract_project_version.xsl
VERSION := $(shell $(XSLTPROC) $(EXTRACT_VERSION) pom.xml)
PROJECT_JAR := target/$(PROJECT_NAME)-$(VERSION).jar
PROJECT_JAR_DEPENDENCIES := target/$(PROJECT_NAME)-$(VERSION)-jar-with-dependencies.jar
PROJECT_BIN := target/$(PROJECT_NAME)-$(VERSION)-bin.zip
GH_PAGES := $(TOP)/gh-pages
ORIGINURL := $(shell git remote get-url origin)

REMOTELOCATOR_XML  := generated_configs/remotelocator.xml
REMOTELOCATOR_HTML := generated_configs/remotelocator.html
REMOTELOCATOR_IRDB := generated_configs/remotelocator_irdb.xml
REMOTELOCATOR_JP1  := generated_configs/remotelocator_jp1.xml
REMOTELOCATOR_LIRC := generated_configs/remotelocator_lirc.xml
REMOTELOCATOR_GIRR := generated_configs/remotelocator_girr.xml
REMOTELOCATOR_FLIPPER := generated_configs/remotelocator_flipper.xml

IRDB_PATH := ../irdb/codes
GIRR_PATH := ../GirrLib/Girr
FLIPPER_PATH := ../Flipper-IRDB
LIRC_PATH := ../../lirc/lirc-remotes/remotes
CLASS=org.harctoolbox.remotelocator.RemoteDatabase
JP1FILE=$(TOP)/src/main/resources/jp1-master-1.18.fods
STYLESHEET=$(TOP)/src/main/xslt/remotelocator2html.xsl

default: $(PROJECT_JAR)

all: $(REMOTELOCATOR_HTML)

help: $(PROJECT_JAR)
	"$(JAVA)" -jar "$(PROJECT_JAR_DEPENDENCIES)" --help

check_all: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--girrdir $(GIRR_PATH) \
	--irdb $(IRDB_PATH) \
	--lirc $(LIRC_PATH) \
	--jp1 "$(JP1FILE)"

check_lirc: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--lirc "$(LIRC_PATH)"

check_jp1: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--jp1 "$(JP1FILE)"

check_irdb: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--irdb $(IRDB_PATH)

check_flipper: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--flipper $(FLIPPER_PATH)

check_girr: $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out /dev/null \
	--girrdir $(GIRR_PATH)

$(REMOTELOCATOR_XML): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--girrdir $(GIRR_PATH) \
	--irdb $(IRDB_PATH) \
	--flipper $(FLIPPER_PATH) \
	--lirc $(LIRC_PATH) \
	--jp1 "$(JP1FILE)"

$(REMOTELOCATOR_LIRC): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--lirc "$(LIRC_PATH)"

$(REMOTELOCATOR_JP1): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--jp1 "$(JP1FILE)"

$(REMOTELOCATOR_IRDB): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--irdb $(IRDB_PATH)

$(REMOTELOCATOR_FLIPPER): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--flipper $(FLIPPER_PATH)

$(REMOTELOCATOR_GIRR): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--girrdir $(GIRR_PATH)

$(REMOTELOCATOR_HTML): $(REMOTELOCATOR_XML)
	$(XSLTPROC)  -o "$@" "${STYLESHEET}"  "$<"

$(PROJECT_JAR) $(PROJECT_BIN):
	mvn install -Dmaven.test.skip=true

$(PROJECT_JAR)-test:
	mvn install -Dmaven.test.skip=false

release: push gh-pages tag deploy

version:
	@echo $(VERSION)

setversion:
	mvn versions:set -DnewVersion=$(NEWVERSION)
	git commit -S -m "Set version to $(NEWVERSION)" pom.xml

deploy:
	mvn deploy -P release

apidoc: target/site/apidocs
	$(BROWSE) $</index.html

javadoc: target/site/apidocs

target/site/apidocs:
	mvn javadoc:javadoc

push:
	git push

gh-pages: target/site/apidocs
	rm -rf $(GH_PAGES)
	git clone --depth 1 -b gh-pages ${ORIGINURL} ${GH_PAGES}
	( cd ${GH_PAGES} ; \
	cp -rf ../target/site/apidocs/* . ; \
	git add * ; \
	git commit -a -m "Update of API documentation" ; \
	git push )

tag:
	git checkout master
	git status
	git tag -s -a Version-$(VERSION) -m "Tagging Version-$(VERSION)"
	git push origin Version-$(VERSION)

clean:
	mvn clean
	rm -rf $(GH_PAGES) generated_configs/remotelocator_* output/*

.PHONY: clean $(PROJECT_JAR)-test release check_all check_lirc check_jp1 check_irdb check_girr
