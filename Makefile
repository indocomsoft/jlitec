SOURCE_DIR := src
OUTPUT_DIR := classes
DEPS_DIR := deps

JFLEX_VERSION := 1.8.2
CUP_VERSION   := 20160615

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
$(all_javas): $(DEPS_DIR)
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

$(DEPS_DIR): jflex cup

.PHONY: jflex
jflex: $(DEPS_DIR)/jflex-full-$(JFLEX_VERSION).jar

$(DEPS_DIR)/jflex-full-$(JFLEX_VERSION).jar: jflex-$(JFLEX_VERSION).tar.gz
	tar -zxvf $< -C $(DEPS_DIR) --strip-components=2 jflex-$(JFLEX_VERSION)/lib/$(@F) || ($(RM) $@; exit 1)

.INTERMEDIATE: jflex-$(JFLEX_VERSION).tar.gz
jflex-$(JFLEX_VERSION).tar.gz:
	$(CURL) https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSION)/$(@F) > $(@F) || ($(RM) $@; exit 1)

.PHONY: cup
cup: $(DEPS_DIR)/java-cup-11b.jar $(DEPS_DIR)/java-cup-11b-runtime.jar

$(DEPS_DIR)/java-cup-11b-runtime.jar $(DEPS_DIR)/java-cup-11b.jar: java-cup-bin-11b-$(CUP_VERSION).tar.gz
	tar -zxvf $< -C $(DEPS_DIR) $(@F) || ($(RM) $@; exit 1)

.INTERMEDIATE: java-cup-bin-11b-$(CUP_VERSION).tar.gz
java-cup-bin-11b-$(CUP_VERSION).tar.gz:
	$(CURL) http://www2.cs.tum.edu/projects/cup/download.php?file=$@ > $@ || ($(RM) $@; exit 1)


.PHONY: clean
clean:
	$(RM) $(OUTPUT_DIR) $(DEPS_DIR)

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR) $(DEPS_DIR))
