# An optimizing Brainf*ck compiler

An optimizing [brainf*ck](http://brainfuck.org/brainfuck.html) compiler with 3 target backends: 
JVM using [ProGuardCORE](https://github.com/guardsquare/proguard-core) for code generation, C and LLVM IR.

Some optimizations are applied before code generation:

* Zero-ing loops (`[+]` / `[-]`) are represented single instruction
* Consecutive zero-ing instructions are merged
* Consecutive move and add instructions are merged into single instructions with an amount parameter
* Empty loops and zero moves/adds are removed
* Top-level loops before memory updates are removed

## Building

```shell
./gradlew build
```

The build task will execute all tests and create an output jar `lib/bf.jar`.

## Executing

A wrapper script `bin/bf` is provided for convenience in the `bin/` directory:

```shell
$ bin/bf --help
Usage: bf options_list
Arguments: 
    script -> brainf*ck script { String }
Options: 
    --output, -o -> output { String }
    --target, -t [JVM] -> target { Value should be one of [jvm, c, llvm] }
    --debug, -d [false] 
    --help, -h -> Usage info
```

By default, `bf` will compile a provided brainf*ck script for the JVM and execute it. The compiler
can instead generate a jar file with the `-o` option.

### JVM (default)
 
```shell
$ bin/bf examples/helloworld.bf -t jvm -o helloworld.jar
$ java -jar helloworld.jar
```

### C

```shell
$ bin/bf examples/helloworld.bf -t c -o helloworld.c
$ gcc helloworld.c -o helloworld && ./helloworld
```

### LLVM IR

```shell
$ bin/bf examples/helloworld.bf -t llvm -o helloworld.ll
$ lli helloworld.ll
```

# Useful brainf*ck resources

* [Brainf*ck language reference](http://brainfuck.org/brainfuck.html)
* [Sample programs by Daniel B Cristofani](http://brainfuck.org/)
* [Optimizing brainf*ck programs](http://calmerthanyouare.org/2015/01/07/optimizing-brainfuck.html)
* [Brainf*ck Wikipedia article](https://en.wikipedia.org/wiki/Brainfuck)
 