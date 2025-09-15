import {createApp} from "vue";
import Map from "./Map.vue";
import OpenLayersMap from "vue3-openlayers";

for (const element of document.querySelectorAll("[data-module='map']")) {
  const app = createApp(Map);
  app.use(OpenLayersMap);
  app.mount(element);
}
