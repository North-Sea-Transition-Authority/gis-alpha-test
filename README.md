# GIS Alpha Test

## Setup

### 1. Initialise the Fivium Design System

```bash
git submodule update --init --recursive
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
```

### 2. Build frontend components

```bash
npm install && npx gulp buildAll
```

### 3. Build the arcgis-js-sdk

run `cd arcgis-js-sdk/ && npm install && npm run copy:core && npm run proto-gen && cd .. ` to install dependencies and build the gRPC proto files.

### 4. Build the backend.

Run the gradle clean and build tasks to generate the java proto classes defined in `src/main/proto`

(Alternatively you can run `npm run build-all` from the root directory to generate build the frontend and generate the proto for
the front and backend.)

### 5. Start the grpc server

Go to `gis-alpha-test/arcgis-js-sdk` and right click `grpc-server.ts` then run.