import {createApp} from "vue";
import Map from "./Map.vue";
import FeatureMap from "./FeatureMap.vue";
import OpenLayersMap from "vue3-openlayers";

for (const element of document.querySelectorAll("[data-module='map']")) {
  const app = createApp(Map);
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='feature-map']")) {
  const app = createApp(FeatureMap, {
    featureIds: element.dataset.featureIds
  });
  app.use(OpenLayersMap);
  app.mount(element);
}
