# Contributing to Zeromile Connect

Thank you for your interest in contributing to Zeromile Connect! This document outlines guidelines and procedures for contributing to this project.

---

## Code of Conduct
We are committed to providing a welcoming, inclusive, and harassment-free experience for everyone. Please be respectful and collaborative in all discussions and pull requests.

---

## Development Workflow

1. **Fork & Branch**:
   - Fork the repository and create a feature branch from `main`:
     ```bash
     git checkout -b feature/my-feature-name
     ```
2. **Coding Standards**:
   - Follow standard Android and Kotlin conventions.
   - Use Jetpack Compose exclusively for UI.
   - Adhere to Material Design 3 (M3) design tokens and color schemes.
   - Ensure all interactive UI elements meet accessibility minimums (48dp touch target) and have descriptive test tags.
3. **Security Rules (CRITICAL)**:
   - NEVER commit `.env` files or hardcode secrets, passwords, or API keys.
   - Always verify that new endpoints or database tables enforce Row-Level Security (RLS).
4. **Testing**:
   - Ensure all unit and integration tests pass before submitting a PR:
     ```bash
     gradle :app:testDebugUnitTest
     ```
5. **Pull Request Submission**:
   - Provide a clear, descriptive title and summary of changes.
   - Reference any related issues or feature specifications.
