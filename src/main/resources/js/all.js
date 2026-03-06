import {createApp} from "vue";
import TestMap from "../templates/gis-alpha-test/testMap/TestMap.vue";
import SplitByPointAndClickPage from "../templates/gis-alpha-test/map/vue/SplitByPointAndClickPage.vue";
import OpenLayersMap from "vue3-openlayers";
import SplitByCoordinateEntryPage from "../templates/gis-alpha-test/map/vue/SplitByCoordinateEntryPage.vue";

for (const element of document.querySelectorAll("[data-module='test-map']")) {
  const app = createApp(TestMap);
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='split-by-point-and-click-page']")) {
  const app = createApp(SplitByPointAndClickPage, {
    featureIds: element.dataset.featureIds,
    srsWkid: Number(element.dataset.srsWkid)
  });
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='split-by-coordinate-entry-page']")) {
  const app = createApp(SplitByCoordinateEntryPage, {
    featureIds: element.dataset.featureIds
  });
  app.use(OpenLayersMap);
  app.mount(element);
}
