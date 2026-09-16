#!/bin/bash

set -e

echo
/usr/bin/lein --version
/usr/bin/lein cljfmt fix

cd jython-apps

echo
/usr/bin/black --version
/usr/bin/black .

echo
~/.local/bin/ruff --version
~/.local/bin/ruff check --ignore F821
