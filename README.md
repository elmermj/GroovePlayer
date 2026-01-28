# GroovePlayer

A modern, high-performance music player for Android built with Jetpack Compose, featuring advanced audio visualization, Bluetooth audio support, and a beautiful, responsive UI.

## 🎵 Features

### Core Playback
- **Music Library Management**: Browse songs, albums, artists, and genres from device storage
- **Queue Management**: Create and manage playlists with shuffle and repeat modes
- **Playback Controls**: Play, pause, skip, seek, and volume control
- **Background Playback**: Continues playing when app is in background via foreground service
- **Media Session Integration**: Full Media3 session support for lock screen and notification controls

### Audio Features
- **5-Band Equalizer**: Real-time audio equalization with custom presets (Normal, Pop, Rock, Jazz, Classic, Dance, Electronic, Hip-Hop)
- **Audio Visualization**: Real-time waveform visualization with multiple modes
- **Beat Detection**: Advanced beat detection algorithm for visual effects
- **Stereo Balance**: Visual representation of stereo audio balance
- **Volume Control**: System volume integration with mute support

### Bluetooth Audio
- **Device Discovery**: Scan and discover Bluetooth audio devices
- **A2DP Support**: Full Advanced Audio Distribution Profile support
- **Connection Management**: Connect/disconnect with visual feedback
- **Real-time Status**: Live connection state monitoring via broadcast receivers
- **Device Types**: Automatic detection of earbuds, headphones, speakers, and phones
- **TWS Support**: Deduplication for True Wireless Stereo earbuds
- **Visual Feedback**: 
  - Pulsating glow animation during connection
  - Green success indicator on successful connection
  - Red failure indicator on connection timeout
  - Elliptical scroll interface for device selection

### Library & Organization
- **Recently Played**: Track and display recently played songs
- **Favorites**: Mark and manage favorite tracks, albums, and artists
- **Search**: Full-text search across songs, albums, artists, and genres
- **Search History**: Recent search suggestions
- **Metadata Editing**: Edit song metadata (title, artist, album, genre)
- **Playback History**: Comprehensive playback tracking

### User Experience
- **Responsive Design**: Adaptive layouts for phones, tablets, and large tablets
- **Dark Theme**: Modern dark theme with custom color schemes
- **Smooth Animations**: Fluid transitions and micro-interactions
- **Custom Shaders**: Advanced RuntimeShader effects for visual elements (API 33+)
- **Canvas Fallbacks**: Graceful degradation for older devices (API 24-32)
- **Edge-to-Edge UI**: Modern edge-to-edge design following Material Design 3

### Technical Features
- **Offline-First**: All music stored locally, no internet required
- **Performance Optimized**: Built with native Kotlin for maximum performance
- **Memory Efficient**: Efficient resource management and lifecycle awareness
- **Permission Handling**: Comprehensive permission management with user-friendly prompts

## 🏗️ Architecture

GroovePlayer follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern, ensuring separation of concerns, testability, and maintainability.

### Architecture Layers

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer                    │
│  (Composables, ViewModels, UI Components)       │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│            Domain Layer                         │
│ (UseCases, Domain Models, Repository Interfaces)│
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│             Data Layer                          │
│  (Repository Implementations, Data Sources)     │
└─────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### **Presentation Layer**
- **Composables**: UI components built with Jetpack Compose
- **ViewModels**: State management and business logic coordination
- **Navigation**: Screen navigation using Navigation Compose
- **UI State**: Reactive state management with StateFlow

#### **Domain Layer**
- **UseCases**: Single-responsibility business logic operations
- **Domain Models**: Pure Kotlin data classes representing business entities
- **Repository Interfaces**: Contracts defining data operations
- **Business Rules**: Core application logic independent of frameworks

#### **Data Layer**
- **Repository Implementations**: Concrete implementations of domain repositories
- **Data Sources**: 
  - **Local**: Room database for persistent storage
  - **MediaStore**: Android MediaStore for music library access
  - **Bluetooth**: Android Bluetooth APIs for audio device management
- **Mappers**: Convert between data and domain models

### Key Design Patterns

1. **Dependency Injection**: Hilt for dependency management
2. **Repository Pattern**: Abstraction over data sources
3. **UseCase Pattern**: Encapsulation of business logic
4. **Observer Pattern**: Reactive programming with Kotlin Flows
5. **CompositionLocal**: Shared ViewModel access via CompositionLocalProvider

## 🛠️ Technology Stack

### Core Framework
- **Kotlin 2.3.0**: Modern, concise programming language
- **Jetpack Compose**: Declarative UI framework
- **Material Design 3**: Modern design system
- **AndroidX Core**: Core Android libraries

### Architecture Components
- **Hilt 2.58**: Dependency injection framework
- **Room 2.8.4**: Local database with Kotlin coroutines support
- **Navigation Compose 2.9.6**: Type-safe navigation
- **Lifecycle**: Lifecycle-aware components

### Media & Audio
- **Media3 ExoPlayer 1.9.0**: High-performance media playback
- **Media3 Session**: Media session management
- **Android Audio Effects**: Equalizer and Visualizer APIs
- **Bluetooth APIs**: A2DP and Headset profile support

### UI & Graphics
- **Compose BOM 2026.01.00**: Latest Compose libraries
- **RuntimeShader**: Advanced shader effects (API 33+)
- **Canvas API**: Custom drawing and fallbacks
- **Coil 3.3.0**: Image loading and caching
- **Lottie 6.4.0**: Animation support

### Additional Libraries
- **Google Fonts**: Custom typography
- **Palette API**: Color extraction from artwork
- **Lucide Icons**: Comprehensive icon set

### Development Tools
- **KSP 2.3.4**: Kotlin Symbol Processing for Room
- **Gradle 8.13.2**: Build automation
- **JUnit & Espresso**: Testing frameworks

## 🎨 Design Choices

### 1. **Clean Architecture**
**Why**: Ensures separation of concerns, testability, and maintainability. Business logic is independent of frameworks, making the codebase easier to test and modify.

**Implementation**:
- Domain layer contains pure Kotlin business logic
- Data layer implements domain interfaces
- Presentation layer depends only on domain layer
- UseCases encapsulate single business operations

### 2. **MVVM Pattern**
**Why**: Provides clear separation between UI and business logic, enables reactive programming, and integrates seamlessly with Jetpack Compose.

**Implementation**:
- ViewModels manage UI state via StateFlow
- Composables observe ViewModel state reactively
- ViewModels use UseCases, never directly access repositories
- Single source of truth for UI state

### 3. **Native Kotlin (Not Flutter)**
**Why**: Maximum performance for audio processing, real-time visualization, and smooth animations. Native access to Android APIs (Bluetooth, Audio Effects, MediaStore).

**Trade-off**: Requires separate codebase for iOS (planned in Swift/SwiftUI), but ensures optimal performance on Android.

### 4. **Jetpack Compose**
**Why**: Modern, declarative UI framework that enables rapid development, type-safe navigation, and excellent performance.

**Benefits**:
- Declarative UI reduces boilerplate
- Built-in state management
- Composable architecture
- Smooth animations and transitions

### 5. **Reactive Programming (Kotlin Flows)**
**Why**: Enables real-time UI updates, efficient state management, and seamless integration with Compose.

**Implementation**:
- StateFlow for UI state
- Flow for async data streams
- Flow operators for data transformation
- Automatic UI updates on state changes

### 6. **Hilt Dependency Injection**
**Why**: Simplifies dependency management, enables testing, and follows Android best practices.

**Benefits**:
- Automatic dependency resolution
- Scoped dependencies (Singleton, Activity, ViewModel)
- Easy testing with test modules
- Reduced boilerplate

### 7. **Room Database**
**Why**: Type-safe, efficient local storage with Kotlin coroutines support.

**Usage**:
- Playback history
- User settings and profile
- Search history
- Song metadata cache
- Favorite tracks/albums/artists

### 8. **RuntimeShader with Canvas Fallback**
**Why**: Advanced visual effects on modern devices while maintaining compatibility with older devices.

**Implementation**:
- RuntimeShader for API 33+ (Android 13+)
- Canvas-based fallback for API 24-32 (Android 7-12)
- Same visual experience across all supported devices
- Graceful degradation

### 9. **CompositionLocal for Shared State**
**Why**: PlayerViewModel needs to be accessible from any screen without prop drilling.

**Implementation**:
- Activity-scoped PlayerViewModel
- Provided via CompositionLocalProvider
- Accessible from any Composable
- Maintains lifecycle awareness

### 10. **Responsive Design**
**Why**: Support multiple screen sizes with optimal layouts.

**Implementation**:
- Device type detection (Phone, Tablet, Large Tablet)
- Separate layouts for each form factor
- Adaptive UI components
- Optimal use of screen real estate

### 11. **Bluetooth Connection Polling**
**Why**: Android Bluetooth broadcasts are unreliable; polling ensures connection state is always accurate.

**Implementation**:
- Poll connection state after connect/disconnect calls
- Broadcast receivers as primary mechanism
- Polling as fallback for missed broadcasts
- Real-time state updates

### 12. **UseCase Pattern**
**Why**: Single responsibility, testability, and clear business logic encapsulation.

**Benefits**:
- Each UseCase does one thing
- Easy to test in isolation
- Clear business logic boundaries
- Reusable across ViewModels

## 📁 Project Structure

```
app/src/main/java/com/aethelsoft/grooveplayer/
├── data/                          # Data Layer
│   ├── bluetooth/                # Bluetooth repository implementation
│   ├── local/                    # Room database, DAOs, entities
│   ├── mapper/                   # Data ↔ Domain model mappers
│   ├── player/                   # ExoPlayer, Equalizer managers
│   └── repository/               # Repository implementations
│
├── domain/                        # Domain Layer
│   ├── model/                    # Domain models (pure Kotlin)
│   ├── repository/               # Repository interfaces
│   └── usecase/                  # Business logic UseCases
│       ├── bluetooth_category/
│       ├── equalizer_category/
│       ├── home_category/
│       ├── player_category/
│       ├── search_category/
│       ├── settings_category/
│       └── user_category/
│
├── presentation/                  # Presentation Layer
│   ├── common/                   # Shared components (LocalPlayerViewModel, etc.)
│   ├── home/                     # Home screen
│   ├── library/                  # Library screens (songs, albums, artists)
│   ├── navigation/               # Navigation setup
│   ├── player/                   # Player screen and components
│   └── search/                   # Search functionality
│
├── services/                      # Background services
│   └── MusicPlaybackService.kt   # Foreground playback service
│
├── utils/                         # Utilities and helpers
│   ├── theme/                    # Custom themes, shaders, icons
│   └── PermissionHandler.kt      # Permission management
│
└── di/                            # Dependency Injection
    ├── AppModule.kt              # App-level dependencies
    └── RepositoryModule.kt       # Repository bindings
```

## 🔑 Key Components

### Player
- **ExoPlayerManager**: Core playback engine with ExoPlayer
- **EqualizerManager**: Real-time audio equalization
- **PlayerViewModel**: Player state management
- **MusicPlaybackService**: Background playback service

### Bluetooth
- **BluetoothRepositoryImpl**: Bluetooth device management
- **BluetoothViewModel**: Connection state management
- **BluetoothEllipticalLazyScroll**: Custom elliptical device selector UI

### Database
- **GroovePlayerDatabase**: Room database with 7 entities
- **DAOs**: Data access objects for all entities
- **Entities**: PlaybackHistory, SongMetadata, UserSettings, etc.

### UI Components
- **Responsive Layouts**: Phone, Tablet, Large Tablet variants
- **Custom Shaders**: Elliptical gradient effects
- **Audio Visualization**: Real-time waveform rendering
- **Custom Sliders**: Volume, seek, equalizer controls

## 📱 Supported Android Versions

- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 14+)
- **Compile SDK**: 36

### API Level Compatibility
- **API 24-32**: Canvas-based fallbacks for shader effects
- **API 33+**: Full RuntimeShader support for advanced visuals
- **All Versions**: Full feature parity with appropriate fallbacks

## 🔮 Future Development

Planned features and directions:

- **Authentication** — User sign-in and account management
- **Profile** — Account and user settings
- **Cloud backup and sync** — For paid users
- **Cloud streaming** — If financially feasible
- **In-app web browser and downloader** — Browse and download music within the app

## 📐 Development & Device Note

GroovePlayer is developed and tested primarily on **Samsung Tab S10 Ultra**. The app is **highly optimized for `DeviceType.LARGE_TABLET`**. Layouts for **`DeviceType.PHONE`** differ significantly and are **not yet fully optimized**; full phone optimization will take additional development time.

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK with API 36
- Gradle 8.13.2+

### Building
```bash
./gradlew assembleDebug
```

### Running
```bash
./gradlew installDebug
```

## 📝 License

**Copyright (c) 2025-2026 Elmer Matthew Japara. All Rights Reserved.**

This project is proprietary software protected by copyright law under the Berne Convention, the laws of the Republic of Indonesia, the laws of the United States of America, and international copyright treaties.

Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without the express written permission of the copyright holder.

## 👤 Author

**Elmer Matthew Japara**

Built with using Kotlin and Jetpack Compose

---

*"The main challenge is designing the UI. Once the design is done and all the features necessary implemented, it would be a cake walk doing this. The design process is without Figma, everything is done on top of my head."*
