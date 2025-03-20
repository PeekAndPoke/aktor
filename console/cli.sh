#!/bin/bash

set -e

./gradlew :run --configuration-cache \
    --console=plain \
    --quiet \
    -Djansi.passthrough=true \
    --args="--cli $*"
