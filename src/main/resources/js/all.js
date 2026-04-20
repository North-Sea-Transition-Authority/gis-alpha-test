import {createApp} from "vue";
import TestMap from "../templates/gis-alpha-test/testMap/TestMap.vue";
import SplitByPointAndClickPage from "../templates/gis-alpha-test/map/vue/SplitByPointAndClickPage.vue";
import OpenLayersMap from "vue3-openlayers";
import SplitByCoordinateEntryPage from "../templates/gis-alpha-test/map/vue/SplitByCoordinateEntryPage.vue";
import InteractiveRenderPage from "../templates/gis-alpha-test/map/vue/InteractiveRenderPage.vue";
import StaticRenderPage from "../templates/gis-alpha-test/map/vue/StaticRenderPage.vue";
import DepthSplitByPointAndClickPage from "../templates/gis-alpha-test/map/vue/DepthSplitByPointAndClickPage.vue";

for (const element of document.querySelectorAll("[data-module='test-map']")) {
  const app = createApp(TestMap);
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='split-by-point-and-click-page']")) {
  const app = createApp(SplitByPointAndClickPage, {
    srsWkid: Number(element.dataset.srsWkid),
    journeyId: element.dataset.journeyId,
  });
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='depth-split-by-point-and-click-page']")) {
  const featureIdAndDepths = JSON.parse(element.dataset.featureIdAndDepths || '[]');
  const app = createApp(DepthSplitByPointAndClickPage, {
    featureId: element.dataset.featureId,
    featureIdAndDepths: featureIdAndDepths,
    srsWkid: Number(element.dataset.srsWkid),
  });
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='split-by-coordinate-entry-page']")) {
  const app = createApp(SplitByCoordinateEntryPage, {
    srsWkid: Number(element.dataset.srsWkid),
    journeyId: element.dataset.journeyId,
    userTestingExtentText: element.dataset.userTestingExtentText,
  });
  app.use(OpenLayersMap);
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='interactive-render-page']")) {
  const shapes = JSON.parse(element.dataset.shapes || '[]');
  const app = createApp(InteractiveRenderPage, {
    shapes: shapes,
  });
  app.mount(element);
}

for (const element of document.querySelectorAll("[data-module='static-render-page']")) {
  const shapes = JSON.parse(element.dataset.shapes || '[]');
  const app = createApp(StaticRenderPage, {
    shapes: shapes,
  });
  app.mount(element);
}
