#!/bin/bash

set -e

/usr/bin/lein cljfmt fix

cd jython-apps

/usr/bin/black .
~/.local/bin/ruff check --ignore E402,F821
