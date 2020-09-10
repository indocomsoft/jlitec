SOURCE_DIR := src
OUTPUT_DIR := classes

MAIN_CLASS := JliteC.Main

FIND  := /usr/bin/find
MKDIR := mkdir -p

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
$(all_javas):
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

.PHONY: clean
clean:
	$(RM) $(OUTPUT_DIR)

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR))
