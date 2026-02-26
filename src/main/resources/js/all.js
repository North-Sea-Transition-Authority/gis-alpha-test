import {createApp} from "vue";
import TestMap from "../templates/gis-alpha-test/testMap/TestMap.vue";
import Map from "../templates/gis-alpha-test/map/vue/Map.vue";
import OpenLayersMap from "vue3-openlayers";

for (const element of document.querySelectorAll("[data-module='test-map']")) {
  const app = createApp(TestMap);
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='map']")) {
  const app = createApp(Map, {
    featureIds: element.dataset.featureIds
  });
  app.use(OpenLayersMap);
  app.mount(element);
}
