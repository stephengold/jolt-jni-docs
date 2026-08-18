#!/bin/bash

set -e

cd jython-apps

/usr/bin/black .
~/.local/bin/ruff check --ignore E402,F821
