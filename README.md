# Omniscient IDE

The Omniscient IDE is an agentic AI development environment built for Android. It bridges the gap between client-side development workflows and powerful server-side AI orchestration.

## Key Features

- **Agentic Collaboration Engine**: Integrates multi-agent feedback flows (e.g., Gemini integration, internal collaboration manager).
- **Thinking-Mode Integration**: Configurable Gemini API calls with custom "Thinking Budgets".
- **Sovereign Agent Dashboard**: A specialized control panel for defining agent constraints, kernel repair settings, and sandbox parameters (memory/CPU limits).
- **Interactive VFS Browser**: Integrated file system manager for project management.
- **Terminal & Command History**: Persistent local terminal shell with command history navigation.
- **Adaptive UI**: Built with Jetpack Compose featuring a retro-cyber "Neon/CyberSlate" aesthetic.
- **Keyboard Shortcuts**: Efficient navigation (Alt+M: Chat, Alt+W: Files, Alt+D: Sovereign).

## Project Status

- **Development Point**: Active development with recent focus on refining agent safety/behavior constraints and terminal usability.
- **Build Status**: Stable compilation.

## Technology Stack

- **Framework**: Android (Jetpack Compose)
- **Networking**: Retrofit + Gemini API
- **Local Storage**: SharedPreferences (Configs), Room (Persistence)
- **Core Architecture**: MVVM, Clean Architecture.

## Production-Signed APK Generation

To generate a production-signed APK, the following GitHub Secrets must be configured in your repository settings:

- `KEYSTORE_BASE64`: The base64-encoded JKS file.
- `KEYSTORE_PASSWORD`: The password for the keystore.
- `KEY_ALIAS`: The alias of the key in the keystore.
- `KEY_PASSWORD`: The password for the key.

These secrets will be automatically used by the `main.yml` workflow during every push to `main` to build and sign the application.
