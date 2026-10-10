#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/apache-maven-3.9.9/bin/mvn" "$@"
