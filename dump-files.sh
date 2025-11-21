#!/bin/bash

# === Static configuration ===
DIR="./app/src/main/java/tn/esprit/data/"  # relative directory
OUT="./all-files-content.txt"                           # output file

# Empty or create output file
> "$OUT"

# Recursively loop through all files
find "$DIR" -type f | while read FILE; do
  echo "===== FILE: $FILE =====" >> "$OUT"
  cat "$FILE" >> "$OUT"
  echo -e "\n" >> "$OUT"
done

echo "Done. Content written to $OUT"
