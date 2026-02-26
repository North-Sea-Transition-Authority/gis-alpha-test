FROM node:22-alpine

WORKDIR /app

COPY arcgis-js-sdk/package*.json .

RUN npm install

COPY src/main/proto /src/main/proto

COPY arcgis-js-sdk/ ./
RUN npm run copy:core && npm run proto-gen

EXPOSE 8082 3000

ENTRYPOINT exec npx tsx grpc-server.ts
