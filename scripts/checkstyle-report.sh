#!/bin/bash
# Script to analyze and categorize checkstyle violations

# Configuration
CHECKSTYLE_VERSION="10.3.3"
CHECKSTYLE_JAR="checkstyle-${CHECKSTYLE_VERSION}-all.jar"
CHECKSTYLE_URL="https://github.com/checkstyle/checkstyle/releases/download/checkstyle-${CHECKSTYLE_VERSION}/${CHECKSTYLE_JAR}"
CHECKSTYLE_CONFIG="checkstyle.xml"
OUTPUT_FILE="checkstyle-report.txt"
SUMMARY_FILE="checkstyle-summary.txt"

# Create directory for tools if it doesn't exist
mkdir -p tools

# Download checkstyle if not already available
if [ ! -f "tools/${CHECKSTYLE_JAR}" ]; then
    echo "Downloading Checkstyle ${CHECKSTYLE_VERSION}..."
    curl -L "${CHECKSTYLE_URL}" -o "tools/${CHECKSTYLE_JAR}"
fi

# Check if config file exists
if [ ! -f "${CHECKSTYLE_CONFIG}" ]; then
    echo "Error: ${CHECKSTYLE_CONFIG} not found!"
    echo "Please create a checkstyle configuration file first."
    exit 1
fi

# Run checkstyle on all Java files
echo "Running checkstyle analysis..."
find . -name "*.java" -not -path "*/target/*" -not -path "*/\.*" | \
    xargs java -jar "tools/${CHECKSTYLE_JAR}" -c "${CHECKSTYLE_CONFIG}" > "${OUTPUT_FILE}"

# Create a summary
echo "Generating summary report..."
echo "CHECKSTYLE VIOLATIONS SUMMARY" > "${SUMMARY_FILE}"
echo "==========================" >> "${SUMMARY_FILE}"
echo "" >> "${SUMMARY_FILE}"

# Count total violations
TOTAL=$(grep -c ":" "${OUTPUT_FILE}")
echo "Total violations: ${TOTAL}" >> "${SUMMARY_FILE}"
echo "" >> "${SUMMARY_FILE}"

# Categorize common violations
echo "VIOLATIONS BY TYPE" >> "${SUMMARY_FILE}"
echo "-----------------" >> "${SUMMARY_FILE}"

# Check for missing newline violations
NEWLINE=$(grep -c "File does not end with a newline" "${OUTPUT_FILE}")
echo "Missing final newline: ${NEWLINE}" >> "${SUMMARY_FILE}"

# Check for line length violations
LINE_LENGTH=$(grep -c "Line is longer than" "${OUTPUT_FILE}")
echo "Line too long: ${LINE_LENGTH}" >> "${SUMMARY_FILE}"

# Check for trailing whitespace
WHITESPACE=$(grep -c "trailing whitespace" "${OUTPUT_FILE}")
echo "Trailing whitespace: ${WHITESPACE}" >> "${SUMMARY_FILE}"

# Check for missing Javadoc issues
JAVADOC=$(grep -c "Javadoc" "${OUTPUT_FILE}")
echo "Javadoc issues: ${JAVADOC}" >> "${SUMMARY_FILE}"

# Check for missing package-info
PACKAGE_INFO=$(grep -c "Missing package-info.java file" "${OUTPUT_FILE}")
echo "Missing package-info.java: ${PACKAGE_INFO}" >> "${SUMMARY_FILE}"

# Check for naming issues
NAMING=$(grep -c "Name '[a-zA-Z0-9_]*' must match pattern" "${OUTPUT_FILE}")
echo "Naming convention issues: ${NAMING}" >> "${SUMMARY_FILE}"

# Other issues
OTHER=$((TOTAL - NEWLINE - LINE_LENGTH - WHITESPACE - JAVADOC - PACKAGE_INFO - NAMING))
echo "Other issues: ${OTHER}" >> "${SUMMARY_FILE}"

echo "" >> "${SUMMARY_FILE}"
echo "FILES WITH MOST VIOLATIONS" >> "${SUMMARY_FILE}"
echo "-----------------------" >> "${SUMMARY_FILE}"
grep ":" "${OUTPUT_FILE}" | cut -d':' -f1 | sort | uniq -c | sort -nr | head -10 >> "${SUMMARY_FILE}"

echo "Analysis complete!"
echo "Detailed report: ${OUTPUT_FILE}"
echo "Summary report: ${SUMMARY_FILE}"
