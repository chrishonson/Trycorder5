# Project Architecture: MVI (Model-View-Intent)

This project follows a strict MVI architecture with a Reactive Store.

## Core Components

### 1. State (Model)
- **Definition**: An immutable data class representing the single source of truth for the screen.
- **Naming**: `[ScreenName]State` (e.g., `ExampleCardState`).
- **Usage**: Must be a `data class`. All properties must be immutable (`val`).

### 2. Intent
- **Definition**: A sealed interface representing all possible user actions or events.
- **Naming**: `[ScreenName]Intent` (e.g., `ExampleCardIntent`).
- **Usage**: Use `data object` for no-arg intents and `data class` for intents with parameters.

### 3. Store (ViewModel)
- **Definition**: A `ViewModel` that holds the `StateFlow` and processes `Intents`.
- **Naming**: `[ScreenName]Store` (e.g., `ExampleCardStore`).
- **Responsibilities**:
    - Expose `val state: StateFlow<State>`.
    - Expose `fun dispatch(intent: Intent)`.
    - Handle business logic and state updates using `_state.update { ... }`.

### 4. UI (View)
- **Definition**: Jetpack Compose functions.
- **Container**: The "Screen" composable (e.g., `ExampleCardScreen`) holds the `Store`, collects state, and dispatches intents.
- **Component**: The "Content" composable (e.g., `ExampleCard`) must be **Pure**. It takes raw data (Strings, Ints, Booleans) as arguments, NOT the State object or the Store.

## Testing Guidelines
- **Unit Tests**: Test the `Store` using `kotlinx-coroutines-test`. Verify that dispatching an `Intent` results in the correct `State` transition.
- **UI Tests**: Use `createAndroidComposeRule` to test the integration of the UI.
