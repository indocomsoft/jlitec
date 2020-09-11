SOURCE_DIR := src/main
OUTPUT_DIR := classes
DEPS_DIR := deps

FIND  := /usr/bin/find
MKDIR := mkdir -p
CURL  := curl -L -s -S
RM    := rm -rf

JFLAGS := -sourcepath $(SOURCE_DIR)	\
	-d $(OUTPUT_DIR)
JVMFLAGS :=

JAVAC := javac
JAVA  := java
JVM   := $(JAVA) $(JVMFLAGS)

.PHONY: all
all: compile

# all_javas - Temp file for holding source file list
all_javas := $(OUTPUT_DIR)/all.javas

.PHONY: compile
compile: $(all_javas) jflex cup
	$(JAVAC) $(JFLAGS) @$<

# all_javas - Gather source file list
.INTERMEDIATE: $(all_javas)
$(all_javas):
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

.PHONY: format
format: $(all_javas) googlejavaformatter
	$(JVM) -jar $(DEPS_DIR)/google-java-format-$(GOOGLE_JAVA_FORMATTER_VERSION)-all-deps.jar --replace @$<

.PHONY: lint
lint: $(all_javas) checkstyle
	$(JVM) -jar $(DEPS_DIR)/checkstyle-$(CHECKSTYLE_VERSION)-all.jar -c /google_checks.xml @$<

# Dependency versions
JFLEX_VERSION                   := 1.8.2
CUP_VERSION                     := 20160615
CHECKSTYLE_VERSION              := 8.36
GOOGLE_JAVA_FORMATTER_VERSION   := 1.9

$(DEPS_DIR): jflex cup checkstyle googlejavaformatter

.PHONY: jflex
jflex: $(DEPS_DIR)/jflex-full-$(JFLEX_VERSION).jar

$(DEPS_DIR)/jflex-full-$(JFLEX_VERSION).jar: jflex-$(JFLEX_VERSION).tar.gz
	tar -zxf $< -C $(DEPS_DIR) --strip-components=2 jflex-$(JFLEX_VERSION)/lib/$(@F) || ($(RM) $@; exit 1)

.INTERMEDIATE: jflex-$(JFLEX_VERSION).tar.gz
jflex-$(JFLEX_VERSION).tar.gz:
	$(CURL) https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSION)/$(@F) > $(@F) || ($(RM) $@; exit 1)

.PHONY: cup
cup: $(DEPS_DIR)/java-cup-11b.jar $(DEPS_DIR)/java-cup-11b-runtime.jar

$(DEPS_DIR)/java-cup-11b-runtime.jar $(DEPS_DIR)/java-cup-11b.jar: java-cup-bin-11b-$(CUP_VERSION).tar.gz
	tar -zxf $< -C $(DEPS_DIR) $(@F) || ($(RM) $@; exit 1)

.INTERMEDIATE: java-cup-bin-11b-$(CUP_VERSION).tar.gz
java-cup-bin-11b-$(CUP_VERSION).tar.gz:
	$(CURL) http://www2.cs.tum.edu/projects/cup/download.php?file=$@ > $@ || ($(RM) $@; exit 1)

.PHONY: checkstyle
checkstyle: $(DEPS_DIR)/checkstyle-$(CHECKSTYLE_VERSION)-all.jar

$(DEPS_DIR)/checkstyle-$(CHECKSTYLE_VERSION)-all.jar:
	$(CURL) https://github.com/checkstyle/checkstyle/releases/download/checkstyle-$(CHECKSTYLE_VERSION)/$(@F) > $@ || ($(RM) $@; exit 1)

.PHONY: googlejavaformatter
googlejavaformatter: $(DEPS_DIR)/google-java-format-$(GOOGLE_JAVA_FORMATTER_VERSION)-all-deps.jar

$(DEPS_DIR)/google-java-format-$(GOOGLE_JAVA_FORMATTER_VERSION)-all-deps.jar:
	$(CURL) https://github.com/google/google-java-format/releases/download/google-java-format-$(GOOGLE_JAVA_FORMATTER_VERSION)/$(@F) > $@ || ($(RM) $@; exit 1)

.PHONY: clean
clean:
	$(RM) $(OUTPUT_DIR) $(DEPS_DIR)

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR) $(DEPS_DIR))
