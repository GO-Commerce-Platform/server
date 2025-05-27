#!/bin/bash
# filepath: scripts/fix-style-basics.sh

echo "Running basic style fixes..."

# Fix files missing final newlines
echo "Fixing missing newlines at end of files..."
find . -name "*.java" -not -path "*/target/*" -not -path "*/\.*" | while read file; do
  if [ "$(tail -c 1 "$file")" != "" ]; then
    echo "Adding newline to $file"
    echo "" >> "$file"
  fi
done

# Fix trailing whitespace ONLY (not trailing letters)
echo "Fixing trailing whitespace..."
find . -name "*.java" -not -path "*/target/*" -not -path "*/\.*" | while read file; do
  # The correct regex to ONLY match spaces and tabs at end of lines
  perl -pi -e 's/[ \t]+$//' "$file"
done

echo "Basic style fixes completed!"
