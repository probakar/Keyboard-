#!/bin/sh
#
# CustomBoard Gradle start-up script for POSIX shells.
#
# This is a self-bootstrapping wrapper: binary artifacts (gradle-wrapper.jar) are not
# committed to the repository, so the first invocation downloads the official wrapper
# jar that matches the version pinned in gradle/wrapper/gradle-wrapper.properties.
# Android Studio and `gradle wrapper` regenerate the same file.
#

APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd -P "$(dirname "$0")" > /dev/null && pwd -P) || exit 1

WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/gradle-wrapper.properties"

warn() { echo "$*" >&2; }
die() {
    echo >&2
    echo "$*" >&2
    echo >&2
    exit 1
}

# ---------------------------------------------------------------------------
# Bootstrap the wrapper jar if it is not present yet.
# ---------------------------------------------------------------------------
if [ ! -f "$WRAPPER_JAR" ]; then
    [ -f "$WRAPPER_PROPERTIES" ] || die "ERROR: $WRAPPER_PROPERTIES not found."
    GRADLE_VERSION=$(sed -n 's|.*gradle-\([0-9][0-9.]*\)-\(bin\|all\)\.zip.*|\1|p' "$WRAPPER_PROPERTIES" | head -n 1)
    [ -n "$GRADLE_VERSION" ] || GRADLE_VERSION="8.7"
    mkdir -p "$WRAPPER_DIR"
    echo "Bootstrapping Gradle wrapper $GRADLE_VERSION ..."
    for TAG in "v${GRADLE_VERSION}.0" "v${GRADLE_VERSION}" "master"; do
        URL="https://raw.githubusercontent.com/gradle/gradle/${TAG}/gradle/wrapper/gradle-wrapper.jar"
        if command -v curl > /dev/null 2>&1; then
            curl -fsSL -o "$WRAPPER_JAR" "$URL" && break
        elif command -v wget > /dev/null 2>&1; then
            wget -q -O "$WRAPPER_JAR" "$URL" && break
        else
            die "ERROR: curl or wget is required to bootstrap the Gradle wrapper, or run 'gradle wrapper' once."
        fi
    done
    [ -s "$WRAPPER_JAR" ] || die "ERROR: unable to download gradle-wrapper.jar. Run 'gradle wrapper --gradle-version $GRADLE_VERSION' instead."
fi

# ---------------------------------------------------------------------------
# Locate a Java runtime.
# ---------------------------------------------------------------------------
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    [ -x "$JAVACMD" ] || die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
else
    JAVACMD="java"
    command -v java > /dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found. Install JDK 17."
fi

# Increase the maximum file descriptors if we can.
if [ "$(uname)" != "Darwin" ] && command -v ulimit > /dev/null 2>&1; then
    MAX_FD=$(ulimit -H -n 2> /dev/null) || MAX_FD=""
    case "$MAX_FD" in
        '' | unlimited | *[!0-9]*) ;;
        *) ulimit -n "$MAX_FD" 2> /dev/null ;;
    esac
fi

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" \
    -classpath "\"$WRAPPER_JAR\"" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

exec "$JAVACMD" "$@"
