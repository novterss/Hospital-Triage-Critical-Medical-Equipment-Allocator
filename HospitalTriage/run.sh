#!/bin/sh
# Compile and run without an IDE. Usage: ./run.sh [--console|--test|--demo]
cd "$(dirname "$0")" || exit 1
rm -rf out && mkdir out
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 --release 17 -d out @out/sources.txt || exit 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out hospital.Main "$@"
