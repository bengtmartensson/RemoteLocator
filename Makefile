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

IRDB_PATH := ../irdb/codes
GIRR_PATH := ../GirrLib/Girr
LIRC_PATH := ../../lirc/lirc-remotes/remotes
CLASS=org.harctoolbox.remotelocator.RemoteDatabase
JP1FILE=$(TOP)/src/test/jp1/jp1-master-1.17.fods
STYLESHEET=$(TOP)/src/main/xslt/remotelocator2html.xsl

default: $(PROJECT_JAR)

all: $(REMOTELOCATOR_HTML)

$(REMOTELOCATOR_XML): $(PROJECT_JAR)
	"$(JAVA)" -cp "$(PROJECT_JAR_DEPENDENCIES)" "$(CLASS)" \
	--out "$@" --sort \
	--girrdir $(GIRR_PATH) \
	--irdb $(IRDB_PATH) \
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
	rm -rf $(GH_PAGES) pom.xml.versionsBackup lirc.xml irdb.xml girrlib.xml remotelocator.*ml generated_configs/*

.PHONY: clean $(PROJECT_JAR)-test release
