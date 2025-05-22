# Contributing to GO-Commerce

Thank you for your interest in contributing to GO-Commerce! This document provides guidelines and instructions for contributing to this project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR-USERNAME/GO-Commerce.git`
3. Set up the development environment following the instructions in the README.md

## Issue Workflow

### Starting an Issue
1. **GitHub Issue Check**:
   - Ensure no one else is already working on the issue
   - Assign issue to yourself

2. **Branch Creation**:
   - Create a feature branch from main using the format: `username/issueXX` (e.g., `aquele-dinho/issue10`)
   - Always base new feature branches on the latest main branch

3. **Initial Analysis**:
   - Review issue requirements and acceptance criteria
   - Check related documentation in the `/wiki` directory
   - Identify affected components and potential impacts

4. **Design Documentation**:
   - For significant features, update or create design documentation
   - Document key design decisions and alternatives considered

### Completing an Issue
1. **Code Quality Check**:
   - Ensure all tests pass (`mvn test`)
   - Verify code meets project's coding standards

2. **Documentation**:
   - Update `CHANGELOG.md` with a summary of changes
   - Update any affected documentation

3. **Version Control**:
   - Create a descriptive commit message explaining the changes

4. **Pull Request**:
   - Create a PR with a clear title referencing the issue number
   - Include a detailed description of changes and testing performed
   - Link the PR to the relevant issue

## Development Guidelines

Follow the coding conventions and architecture principles outlined in the COPILOT.md document.

### Core Principles
- Follow SOLID principles
- Prefer composition over inheritance
- Validate all user inputs at the API boundary
- Avoid overengineering - choose the simplest approach that solves the problem
- Write comprehensive tests for all code

## License

By contributing to GO-Commerce, you agree that your contributions will be licensed under the project's dual license as specified in the LICENSE file.