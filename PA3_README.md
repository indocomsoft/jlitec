# JLite Compiler

## Requirements
- Java 14
- Bash shell
- Connection to the internet

This project uses the gradle build system, which can self-bootstrap via
`gradle/wrapper/graddle-wrapper.jar`.
As such, all the `Makefile` does is invoke gradle to build the jar artifact.
If gradle has not been set up before, the self-bootstrapping process will
download all the requirements, freeing you from having to install anything
besides Java 14.

## Usage

Usage without optimisation:
```bash
./jlitec/jlitec arm sample/fraction_fixed.j
```

Usage with optimisation:
```bash
./jlitec/jlitec arm -O3 sample/fraction_fixed.j
```

## Subdirectories

```
|- jlitec   : Project code
`- sample   : 5 sample programs tried on the product
```

## Sample
There are 5 sample programs I tried on my compiler.

The source codes are the files with extension *.j
The compiler's assembly output are the files with extension *.s
The compiler's optimised assembly output are the files with extension *.opt.s
Statically assembled and linked binaries are the files with extension *.out
The expected output for each binary are in the files with extension *.expectedoutput

The 5 sample files are:
- div_fixed.j      : Implementation of division with quotient
- fraction_fixed.j : Implementation of a fraction class with fraction simplication
- gcd_fixed.j      : Implementation of Euclid's GCD algorithm (greatest common divisor)
- list_fixed.j     : Implementation of a linked list
- matrix_fixed.j   : Implementation of 4x4 by 4x4 matrix multiplication

## Extra sample files
If you are interested in more sample files, especially ones with interactive I/O,
find more in jlitec/test/arm/

I recommend trying out list.j, which is an interactive program to manipulate
a linked list.

To compile, first set $CC_ARM with the gcc cross-compiler for ARM,
and $GEM5 with the gem5 base directory, for example, in my macOS-based system:
```bash
CC_ARM=arm-unknown-linux-gnueabi-gcc
GEM5=~/GitHub/gem5/
```

Then simply run the following from the current directory:
```bash
./jlitec/jlitec arm jlitec/test/arm/list.j > /tmp/a.s
$CC_ARM --static /tmp/a.s -o /tmp/a.out
$GEM5/build/ARM/gem5.opt $GEM5/configs/example/se.py -c /tmp/a.out -i /dev/tty
```
