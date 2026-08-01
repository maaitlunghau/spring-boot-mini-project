#!/usr/bin/env sh
set -eu

if [ ! -f .env ]; then
  echo "Error: .env not found. Copy .env.example to .env and fill in real values first."
  exit 1
fi

export $(grep -v '^#' .env | xargs)
exec ./mvnw spring-boot:run
