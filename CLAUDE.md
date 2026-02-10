# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Full Build
```bash
npm run build-all  # Gradle clean + protobuf generation + ArcGIS SDK proto generation
```

### Frontend Only
```bash
npm install                    # Install root dependencies
cd fivium-design-system-core && npm install && cd ..  # Install FDS submodule dependencies
gulp buildAll                  # Build SASS + JS bundle (includes FDS copy)
gulp rollup-babel              # Build JS bundle only
```

### Backend Only
```bash
./gradlew bootRun              # Run Spring Boot app
./gradlew generateProto        # Generate gRPC/protobuf code
```

### Tests
```bash
./gradlew test                 # Java unit tests (JUnit 5)
./gradlew testIntegration      # Integration tests (requires Docker for TestContainers)
npm run vue-test               # Vue component tests (Jest)
cd arcgis-js-sdk && npm test   # ArcGIS SDK tests (Vitest)
```

### Linting
```bash
cd fivium-design-system-core && gulp lint  # ESLint (Airbnb + Vue3)
```

## Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5, Java 17+, Freemarker templates, Flyway migrations
- **Frontend**: Vue 3, OpenLayers 10 (via vue3-openlayers), Rollup bundler
- **Database**: PostgreSQL (primary), Oracle support
- **Spatial Libraries**: ESRI Geometry API 2.2.4, JTS 1.20.0
- **Communication**: gRPC with Protobuf code generation

### Data Model
Features follow a normalized spatial hierarchy stored in PostgreSQL:
- `features` → `polygons` → `lines`
- All entities use UUID primary keys
- Attributes stored as JSONB

### Key Directories
- `src/main/java/uk/co/fivium/gisalphatest/` - Java backend
  - `feature/` - Core domain entities (Feature, Polygon, Line) and repositories
  - `mvc/` - Controllers, ReverseRouter, error handling
  - `oracle/` - Legacy Oracle database integration
  - `arcgis/` - ArcGIS service integration
  - `transformations/` - Geometry operations (split, etc.)
  - `grpc/` - gRPC client services
- `src/main/resources/js/` - Vue frontend (Map.vue is the main map component)
- `src/main/resources/db/migration/` - Flyway SQL migrations
- `arcgis-js-sdk/` - TypeScript wrapper for ArcGIS JavaScript SDK with gRPC proto generation
- `fivium-design-system-core/` - Git submodule for GOV.UK-based design system

### Frontend Build Pipeline
1. FDS resources copied to `src/main/resources/templates/fds`
2. SASS compiled from `src/main/resources/scss/` to `public/assets/static/css/`
3. JS bundled via Rollup from `src/main/resources/js/all.js` to `public/assets/static/js/gis-alpha-test-bundle.js`

### Coordinate Systems
The frontend uses EPSG:3857 (Web Mercator) for display and transforms to/from EPSG:4326 (WGS84) for calculations.

## Setup Requirements

1. Initialize git submodule: `git submodule update --init --recursive`
2. Docker required for integration tests (TestContainers)
3. Protobuf compilation required before backend build
