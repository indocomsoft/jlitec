SOURCE_DIR := src
OUTPUT_DIR := classes
DEPS_DIR := deps

JFLEX_VERSION := 1.8.2

FIND  := /usr/bin/find
MKDIR := mkdir -p
CURL  := curl -L
RM    := rm -rf

JAVAC := JAVAC

JFLAGS := -sourcepath $(SOURCE_DIR)	\
	-d $(OUTPUT_DIR) \

.PHONY: all
all: compile

# all_javas - Temp file for holding source file list
all_javas := $(OUTPUT_DIR)/all.javas

.PHONY: compile
compile: $(all_javas)
	$(JAVAC) $(JFLAGS) @$<

# all_javas - Gather source file list
.INTERMEDIATE: $(all_javas)
$(all_javas): deps
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

deps: jflex

.PHONY: jflex
jflex: deps/jflex-full-$(JFLEX_VERSION).jar

deps/jflex-full-$(JFLEX_VERSION).jar: jflex-$(JFLEX_VERSION).tar.gz
	tar -zxvf jflex-$(JFLEX_VERSION).tar.gz -C deps --strip-components=2 jflex-$(JFLEX_VERSION)/lib/jflex-full-$(JFLEX_VERSION).jar || (rm $@; exit 1)

.INTERMEDIATE: jflex-$(JFLEX_VERSION).tar.gz
jflex-$(JFLEX_VERSION).tar.gz:
	$(CURL)  https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSION)/jflex-$(JFLEX_VERSION).tar.gz > $@ || (rm $@; exit 1)

.PHONY: clean
clean:
	$(RM) $(OUTPUT_DIR) $(DEPS_DIR)

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR) $(DEPS_DIR))
