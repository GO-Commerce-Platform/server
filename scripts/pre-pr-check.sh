#!/bin/bash
set -e

echo "🔍 Running Pre-PR Validation Checks"
echo "===================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Track overall status
STATUS=0

# Check for recent documentation updates
echo -n "📚 Checking wiki documentation... "
WIKI_FILES=$(find wiki -type f -name "*.md" -mtime -1 | wc -l)
if [ $WIKI_FILES -gt 0 ]; then
    echo -e "${GREEN}✓ Wiki documentation has been updated recently${NC}"
else
    echo -e "${YELLOW}⚠️ No recent wiki updates found${NC}"
    echo "   Consider updating documentation in the wiki directory"
    # Not failing the check, just a warning
fi

# Run tests
echo -n "🧪 Running tests... "
mvn test > test_output.log 2>&1
TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed${NC}"
    # Check for specific warnings in the output
    if grep -q "TenantResolver" test_output.log; then
        echo -e "${YELLOW}⚠️ Warning: TenantResolver needs @PersistenceUnitExtension qualifier${NC}"
    fi
    if grep -q "Error during Logout" test_output.log; then
        echo -e "${YELLOW}⚠️ Warning: Keycloak logout errors detected${NC}"
    fi
else
    echo -e "${RED}✗ Tests failed${NC}"
    grep -A 10 "FAILURE\|ERROR" test_output.log || echo "No specific error details found"
    STATUS=1
fi

# Check code style
echo -n "🎨 Checking code style... "
mvn checkstyle:check > style_output.log 2>&1
STYLE_RESULT=$?
if [ $STYLE_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ Code style is compliant${NC}"
else
    echo -e "${YELLOW}⚠️ Checkstyle issues found (will not fail the build)${NC}"
    STYLE_VIOLATIONS=$(grep -c "\[ERROR\]" style_output.log)
    echo "   Found $STYLE_VIOLATIONS style violations. These should be addressed in a separate PR."
fi

# Get changed files compared to main branch
echo -n "🔍 Detecting changed files... "
CHANGED_FILES=$(git diff --name-only main...HEAD)
echo "Found $(echo "$CHANGED_FILES" | wc -l | tr -d ' ') changed files"

# DYNAMIC ENTITY CHECKS
echo "🏗️ Checking component structure..."
# Find all modified/added entity files
ENTITY_FILES=$(echo "$CHANGED_FILES" | grep -E "src/main/java/.*/entity/[A-Z][^/]+\.java$" || echo "")

if [ -n "$ENTITY_FILES" ]; then
    echo "   Found entity changes, checking for required components..."
    
    for entity_file in $ENTITY_FILES; do
        # Extract entity name (filename without extension)
        ENTITY_NAME=$(basename "$entity_file" .java)
        echo "   Checking components for entity: $ENTITY_NAME"
        
        # Extract package path
        PKG_PATH=$(dirname "$entity_file" | sed 's|src/main/java/||' | sed 's|/entity||' | tr '/' '.')
        BASE_PATH=$(dirname "$entity_file" | sed 's|/entity||')
        
        # Define expected files for this entity
        COMPONENT_PATHS=(
            "${BASE_PATH}/repository/${ENTITY_NAME}Repository.java"
            "${BASE_PATH}/service/.*Service.java"
            "${BASE_PATH}/service/impl/.*ServiceImpl.java"
            "${BASE_PATH}/api/.*Resource.java"
            "${BASE_PATH}/api/dto/.*Request.java"
            "${BASE_PATH}/api/dto/.*Response.java"
        )
        
        # Check for migration files if this is a new entity
        if ! git diff --name-only origin/main..HEAD | grep -q "$entity_file"; then
            echo "   Checking for migration script for new entity: $ENTITY_NAME"
            if ! echo "$CHANGED_FILES" | grep -q "src/main/resources/db/migration/.*\.sql"; then
                echo -e "${YELLOW}⚠️ Warning: No migration script found for new entity $ENTITY_NAME${NC}"
            fi
        fi
        
        # Check for each expected component
        for path_pattern in "${COMPONENT_PATHS[@]}"; do
            if ! echo "$CHANGED_FILES" | grep -q -E "$path_pattern"; then
                echo -e "${YELLOW}⚠️ Warning: Couldn't find matching component for pattern: $path_pattern${NC}"
            fi
        done
    done
else
    echo "   No entity changes detected, skipping component structure check"
fi

# SECURITY ANNOTATION CHECKS
echo -n "🔒 Checking security annotations... "
RESOURCE_FILES=$(echo "$CHANGED_FILES" | grep -E "src/main/java/.*/api/.*Resource.java$" || echo "")

if [ -n "$RESOURCE_FILES" ]; then
    MISSING_SECURITY=0
    
    for resource_file in $RESOURCE_FILES; do
        if ! grep -q -E "@RolesAllowed|@PermitAll" "$resource_file"; then
            echo -e "${RED}✗ Missing security annotations in: $resource_file${NC}"
            MISSING_SECURITY=1
        fi
    done
    
    if [ $MISSING_SECURITY -eq 0 ]; then
        echo -e "${GREEN}✓ Security annotations present in all resource files${NC}"
    else
        echo -e "${RED}✗ Some resources are missing security annotations${NC}"
        STATUS=1
    fi
else
    echo -e "${YELLOW}⚠️ No resource files changed, skipping security check${NC}"
fi

# CHECK FOR DTO RECORDS
echo -n "📦 Checking DTO implementation... "
DTO_FILES=$(echo "$CHANGED_FILES" | grep -E "src/main/java/.*/api/dto/.*\.java$" || echo "")

if [ -n "$DTO_FILES" ]; then
    INVALID_DTOS=0
    
    for dto_file in $DTO_FILES; do
        if ! grep -q "record " "$dto_file"; then
            echo -e "${RED}✗ DTO not implemented as record: $dto_file${NC}"
            INVALID_DTOS=1
        fi
    done
    
    if [ $INVALID_DTOS -eq 0 ]; then
        echo -e "${GREEN}✓ All DTOs implemented as records${NC}"
    else
        echo -e "${RED}✗ Some DTOs are not implemented as records${NC}"
        STATUS=1
    fi
else
    echo -e "${YELLOW}⚠️ No DTO files changed, skipping check${NC}"
fi

# SUGGEST PR COMMAND
echo "===================================="
if [ $STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Ready for PR.${NC}"
    
    # Extract issue number from branch name or commit message
    BRANCH_NAME=$(git branch --show-current)
    ISSUE_NUM=$(echo "$BRANCH_NAME" | grep -o '#[0-9]\+' || echo "$BRANCH_NAME" | grep -o 'issue-[0-9]\+' | sed 's/issue-/#/g')
    
    if [ -z "$ISSUE_NUM" ]; then
        ISSUE_NUM=$(git log -1 --pretty=%B | grep -o '#[0-9]\+' | head -1)
    fi
    
    # Generate PR title and body based on most recent commit
    COMMIT_MSG=$(git log -1 --pretty=%B | head -1)
    
    if [ -n "$ISSUE_NUM" ]; then
        echo ""
        echo "You can create a PR with:"
        echo "gh pr create --title \"$COMMIT_MSG\" \\"
        echo "  --body \"Implements $ISSUE_NUM\" \\"
        echo "  --assignee @me"
    else
        echo ""
        echo "You can create a PR with:"
        echo "gh pr create --title \"$COMMIT_MSG\" \\"
        echo "  --body \"Implements [feature/fix description]\" \\"
        echo "  --assignee @me"
    fi
else
    echo -e "${RED}❌ Some checks failed. Please fix the issues before creating a PR.${NC}"
fi

# Clean up temporary files
rm -f test_output.log style_output.log

exit $STATUS