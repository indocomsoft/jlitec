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

# Dependency versions
JFLEX_VERSION                   := 1.8.2
CUP_VERSION                     := 20160615
CHECKSTYLE_VERSION              := 8.36
GOOGLE_JAVA_FORMATTER_VERSION   := 1.9

$(DEPS_DIR): jflex cup checkstyle googlejavaformatter

# all_javas - Temp file for holding source file list
all_javas := $(OUTPUT_DIR)/all.javas

# format_javas - Temp file for holding source file list to be formatted
format_javas := $(OUTPUT_DIR)/format.javas

# JAR files
jflex_jar := $(DEPS_DIR)/jflex-full-$(JFLEX_VERSION).jar
cup_jar := $(DEPS_DIR)/java-cup-11b.jar
cup_runtime_jar := $(DEPS_DIR)/java-cup-11b-runtime.jar

# JFLEX
jflex_input := $(SOURCE_DIR)/jflex/jlite.flex
jflex_output := $(SOURCE_DIR)/jlitec/Lexer.java
$(jflex_output): $(jflex_input) $(jflex_jar)
	$(JVM) -jar $(jflex_jar) --nobak -d $(@D) $<

# CUP
cup_input := $(SOURCE_DIR)/cup/jlite.cup
cup_output_sym    := $(SOURCE_DIR)/jlitec/sym.java
cup_output_parser := $(SOURCE_DIR)/jlitec/parser.java
$(cup_output_sym): $(cup_input) $(cup_jar)
	$(JVM) -jar $(cup_jar) -destdir $(@D) $<
$(cup_output_parser): $(cup_output_sym)

.PHONY: compile
compile: $(all_javas) $(cup_runtime_jar)
	$(JAVAC) $(JFLAGS) -classpath $(cup_runtime_jar) @$<

.PHONY: compilejflex
compilejflex: $(jflex_output)

.PHONY: compilecup
compilecup: $(cup_output_sym) $(cup_output_parser)

# all_javas - Gather source file list
.INTERMEDIATE: $(all_javas)
$(all_javas): $(jflex_output) $(cup_output_sym) $(cup_output_parser)
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

# format_javas - Skip jflex and cup outputs
.INTERMEDIATE: $(format_javas)
$(format_javas): $(all_javas)
	cat $< \
		| grep -v '^$(jflex_output)$$' \
		| grep -v '^$(cup_output_sym)$$' \
		| grep -v '^$(cup_output_parser)$$' \
		> $@

.PHONY: format
format: $(format_javas) googlejavaformatter
	$(JVM) -jar $(DEPS_DIR)/google-java-format-$(GOOGLE_JAVA_FORMATTER_VERSION)-all-deps.jar --replace @$<

.PHONY: lint
lint: $(format_javas) checkstyle
	$(JVM) -jar $(DEPS_DIR)/checkstyle-$(CHECKSTYLE_VERSION)-all.jar -c /google_checks.xml @$<

.PHONY: jflex
jflex: $(jflex_jar)

$(jflex_jar): jflex-$(JFLEX_VERSION).tar.gz
	tar -zxf $< -C $(DEPS_DIR) --strip-components=2 jflex-$(JFLEX_VERSION)/lib/$(@F) || ($(RM) $@; exit 1)

.INTERMEDIATE: jflex-$(JFLEX_VERSION).tar.gz
jflex-$(JFLEX_VERSION).tar.gz:
	$(CURL) https://github.com/jflex-de/jflex/releases/download/v$(JFLEX_VERSION)/$(@F) > $(@F) || ($(RM) $@; exit 1)


.PHONY: cup
cup: $(cup_jar) $(DEPS_DIR)/java-cup-11b-runtime.jar

$(cup_runtime_jar) $(cup_jar): java-cup-bin-11b-$(CUP_VERSION).tar.gz
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

.PHONY: superclean
superclean:
	$(RM) $(OUTPUT_DIR) $(DEPS_DIR) $(jflex_output) $(cup_output_sym) $(cup_output_parser)

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR) $(DEPS_DIR))
