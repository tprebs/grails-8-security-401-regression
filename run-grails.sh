#!/usr/bin/env bash
#
# Runs the reproduction against one Grails line.
#
# Grails 7 and Grails 8 need different Gradle versions - the Grails 7 Gradle plugin is incompatible with
# Gradle 9, and Grails 8 requires Gradle 9.7+ - so the wrapper is switched along with the dependency
# versions. Both are restored on exit, leaving the working tree as it was found.
#
# Usage: ./run-grails.sh 7 | ./run-grails.sh 8
set -euo pipefail

LINE="${1:-}"
case "$LINE" in
    7) GRAILS=7.2.3 ; GRADLE=8.11.1 ; EXPECT=pass ;;
    8) GRAILS=8.0.0 ; GRADLE=9.8.0  ; EXPECT=fail ;;
    *) echo "usage: $0 7|8" >&2 ; exit 2 ;;
esac
shift   # drop the line selector so any remaining arguments pass through to Gradle

cd "$(dirname "$0")"
PROPS=gradle.properties
WRAPPER=gradle/wrapper/gradle-wrapper.properties
cp "$PROPS" "$PROPS.bak"
cp "$WRAPPER" "$WRAPPER.bak"
restore() { mv "$PROPS.bak" "$PROPS"; mv "$WRAPPER.bak" "$WRAPPER"; }
trap restore EXIT

sed -i.tmp -E "s/^grailsVersion=.*/grailsVersion=$GRAILS/; s/^springSecurityVersion=.*/springSecurityVersion=$GRAILS/; s/^springSecurityRestVersion=.*/springSecurityRestVersion=$GRAILS/" "$PROPS"
sed -i.tmp -E "s|gradle-[0-9.]+-bin\.zip|gradle-$GRADLE-bin.zip|" "$WRAPPER"
rm -f "$PROPS.tmp" "$WRAPPER.tmp"

echo
echo "=============================================================="
echo "  Grails $GRAILS   (Gradle $GRADLE)"
echo "  a @Secured controller, requested with no credentials"
echo "  expecting the test to $EXPECT"
echo "=============================================================="
echo

rm -rf build/test-results
set +e
./gradlew integrationTest --console=plain "$@" 2>&1 | grep -vE '^\s*$'
STATUS=${PIPESTATUS[0]}
set -e

echo
if [ "$EXPECT" = "pass" ] && [ "$STATUS" -eq 0 ]; then
    echo "  RESULT: passed - Grails $GRAILS answers the unauthenticated request properly."
elif [ "$EXPECT" = "fail" ] && [ "$STATUS" -ne 0 ]; then
    echo "  RESULT: failed - Grails $GRAILS answers 500. This is the defect."
else
    echo "  RESULT: UNEXPECTED for Grails $GRAILS - read the output above rather than trusting this line."
fi
echo
