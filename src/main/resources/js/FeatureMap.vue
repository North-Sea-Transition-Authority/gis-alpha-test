<template>
  <ol-map class="map" ref="mapRef" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" />

    <ol-tile-layer>
      <ol-source-osm />
    </ol-tile-layer>

    <ol-vector-layer :style="featureStyle" :declutter="true">
      <ol-source-vector :url="'/feature-map/geojson'" :format="geoJson" @featuresloadend="fitToExtent">
      </ol-source-vector>
    </ol-vector-layer>
  </ol-map>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import GeoJSON from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import {Fill, Stroke, Style, Text} from 'ol/style';

const mapRef = ref(null);
const geoJson = new GeoJSON();
const quadrantUrl = buildServiceUrl('UKCS_quadrants_(WGS84)', 'QUADRANT');
const quadBlockUrl = buildServiceUrl('UKCS_blocks_(WGS84)', 'BLOCK_REF');

onMounted(() => {
  const map = mapRef.value?.map;
  if (!map) {
    return;
  }

  map.addLayer(new VectorLayer({
    source: createPaginatedVectorSource(quadrantUrl),
    style: quadrantStyle,
    declutter: true,
  }));

  map.addLayer(new VectorLayer({
    source: createPaginatedVectorSource(quadBlockUrl),
    style: quadBlockStyle,
    declutter: true,
  }));
});

function buildServiceUrl(resourcePath, outFields = '') {
  const serviceBase = 'https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/';
  const querySuffix = '/FeatureServer/0/query?where=1%3D1&f=geojson';
  const encodedOutFields = outFields ? `&outFields=${encodeURIComponent(outFields)}` : '';
  return `${serviceBase}${resourcePath}${querySuffix}${encodedOutFields}`;
}

//There is a 2000 feature limit on each API call, so we need to paginate them.
function createPaginatedVectorSource(serviceUrl) {
  return new VectorSource({
    format: geoJson,
    loader: async function (extent, resolution, projection) {
      const urlObj = new URL(serviceUrl);
      const format = this.getFormat();
      let offset = 0;
      let hasMore = true;

      try {
        while (hasMore) {
          urlObj.searchParams.set('resultOffset', offset);
          const response = await fetch(urlObj.toString());
          if (!response.ok) {
            throw new Error(`Failed to fetch: ${response.status}`);
          }
          const data = await response.json();
          const features = format.readFeatures(data, {featureProjection: projection});
          this.addFeatures(features);
          offset += features.length;
          hasMore = data?.properties?.exceededTransferLimit === true;
        }
      } catch (error) {
        console.error('Error loading vector source:', error);
        this.removeLoadedExtent(extent);
      }
    },
  });
}

const quadrantStyle = new Style({
  fill: new Fill({color: 'rgba(0, 0, 0, 0)'}),
  stroke: new Stroke({color: 'rgba(0, 46, 109, 0.8)', width: 1.5}),
});

const quadBlockStyle = new Style({
  fill: new Fill({color: 'rgba(0, 0, 0, 0)'}),
  stroke: new Stroke({color: 'rgba(0, 46, 109, 0.8)', width: 0.5}),
});

function featureStyle(feature) {
  return new Style({
    stroke: new Stroke({color: 'rgba(0, 100, 210, 0.8)', width: 2}),
    fill: new Fill({color: 'rgba(0, 100, 210, 0.15)'}),
    text: new Text({
      text: feature.get('featureName') || '',
      font: '14px sans-serif',
      fill: new Fill({color: '#000'}),
      stroke: new Stroke({color: '#fff', width: 3}),
      overflow: true,
    }),
  });
}

function fitToExtent(event) {
  const map = mapRef.value?.map;
  if (!map) {
    return;
  }

  const source = event.target;
  const extent = source.getExtent();

  if (extent && isFinite(extent[0])) {
    map.getView().fit(extent, {
      padding: [50, 50, 50, 50],
    });
  }
}
</script>

<style scoped>
  .map {
    aspect-ratio: 1 / 1;
  }
</style>
