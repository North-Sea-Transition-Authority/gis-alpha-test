# GIS Alpha Test

## Setup


### Profiles
When developing locally, use the `development` profile, to access the oracle database and migrate data from there.

### Environment variables
Needed for production:

| Environment Variabl        | Description                                                                        |
|----------------------------|------------------------------------------------------------------------------------|
| `USER_TESTING_RESET_TOKEN` | This is the token that allows the database to be reset to fresh user testing data. |

### 1. Initialise the Fivium Design System

```bash
git submodule update --init --recursive
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
```

### 2. Build frontend components

```bash
npm install && npx gulp buildAll
```

### 3. Build the app.
The backend contains two parts; the java app and the node server. 
The java app is responsible for serving the frontend, but also critically getting data from the database and
communicating with the node server via gRPC to perform operations on the data.
gRPC uses protobuf to define messages and types which are generated and used on both the java and node app.

Additionally, the frontend also needs to build the vue components before they can be served.

To build everything you can run `npm run build-all` from the root directory to generate the proto for both apps and build the 
frontend vue components.

Alternatively, you can build the parts separately by:
- running `cd arcgis-js-sdk/ && npm install && npm run copy:core && npm run proto-gen && cd .. ` to install dependencies and build the gRPC proto files.
- running the gradle clean and build tasks to generate the java proto classes defined in `src/main/proto`
- running `gulp rollup-babel` to build the frontend vue components

### 4. Start the node server

Go to `gis-alpha-test/arcgis-js-sdk` and right click `grpc-server.ts` then run