#!/bin/bash
# filepath: scripts/generate-package-info.sh

echo "Creating missing package-info.java files..."

# Function to extract package name from a Java file
function extract_package() {
  grep -m 1 "package " "$1" | sed 's/package \(.*\);/\1/'
}

# Find all directories with Java files that don't have package-info.java
find . -name "*.java" -not -path "*/target/*" -not -path "*/\.*" | while read file; do
  dir=$(dirname "$file")
  package_info="$dir/package-info.java"

  if [ ! -f "$package_info" ]; then
    # Get package name from the first Java file in the directory
    package_name=$(extract_package "$file")

    if [ ! -z "$package_name" ]; then
      echo "Creating package-info.java for $package_name in $dir"

      # Create package-info file with basic documentation
      cat > "$package_info" << EOF
/**
 * This package contains classes related to the ${package_name##*.} functionality.
 *
 * @since 1.0
 */
package $package_name;
EOF
    fi
  fi
done

echo "Package info file generation completed!"
