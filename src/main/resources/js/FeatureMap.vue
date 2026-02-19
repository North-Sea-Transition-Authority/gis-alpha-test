<template>
  <ol-map class="map" ref="mapRef" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2"/>

    <ol-tile-layer>
      <ol-source-osm />
    </ol-tile-layer>

    <ol-vector-layer :style="featureStyle" :declutter="true">
      <ol-source-vector :url="'/feature-map/esrijson'" :format="esriJson" @featuresloadend="fitToExtent">
      </ol-source-vector>
    </ol-vector-layer>

    <ol-vector-layer>
      <ol-source-vector>
        <ol-feature v-for="point in points" :key="point.id">
          <ol-geom-point :coordinates="point.wgs84Coordinates" />
          <ol-style>
            <ol-style-circle :radius="3">
              <ol-style-fill color="grey" />
            </ol-style-circle>
          </ol-style>
        </ol-feature>
      </ol-source-vector>
    </ol-vector-layer>
  </ol-map>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import proj4 from 'proj4';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import {Fill, Stroke, Style, Text} from 'ol/style';
import {EsriJSON} from "ol/format";
import {useGeographic} from "ol/proj";
import {debounce} from './debounce';

//definition for ED50 (EPSG:4230) as per https://spatialreference.org/ref/epsg/4230/
proj4.defs('EPSG:4230', '+proj=longlat +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +no_defs');

const mapRef = ref(null);
const esriJson = new EsriJSON();
const quadrantUrl = buildServiceUrl('UKCS_quadrants_(WGS84)', 'QUADRANT');
const quadBlockUrl = buildServiceUrl('UKCS_blocks_(WGS84)', 'BLOCK_REF');
const MIN_SNAP_ZOOM = 11;

const points = ref([]);

//allow us to use geographic coordinates on the map
useGeographic()

const regeneratePoints = () => {
  const map = mapRef.value?.map;
  if (!map) return;

  //Only generate points above this zoom level, to avoid running out of memory.
  if (map.getView().getZoom() < MIN_SNAP_ZOOM) {
    points.value = [];
    return;
  }

  // useGeographic() makes calculateExtent return WGS84 (EPSG:4326)
  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());

  // Transform WGS84 viewport bounds to ED50 (EPSG:4230)
  const [ed50MinLon, ed50MinLat] = proj4('EPSG:4326', 'EPSG:4230', [wgs84MinLon, wgs84MinLat]);
  const [ed50MaxLon, ed50MaxLat] = proj4('EPSG:4326', 'EPSG:4230', [wgs84MaxLon, wgs84MaxLat]);

  const arcSecondSpacing = 30 / 3600; // 30 arc seconds in decimal degrees
  const ed50StartLon = Math.floor(ed50MinLon / arcSecondSpacing) * arcSecondSpacing;
  const ed50StartLat = Math.floor(ed50MinLat / arcSecondSpacing) * arcSecondSpacing;

  const pointsArray = [];
  for (let ed50Lon = ed50StartLon; ed50Lon <= ed50MaxLon; ed50Lon += arcSecondSpacing) {
    for (let ed50Lat = ed50StartLat; ed50Lat <= ed50MaxLat; ed50Lat += arcSecondSpacing) {
      // Transform ED50 (EPSG:4230) to WGS84 (EPSG:4326) for display
      const [wgs84Lon, wgs84Lat] = proj4('EPSG:4230', 'EPSG:4326', [ed50Lon, ed50Lat]);
      pointsArray.push({
        id: `${ed50Lon},${ed50Lat}`,
        wgs84Coordinates: [wgs84Lon, wgs84Lat],
        ed50Coordinates: [ed50Lon, ed50Lat],
      });
    }
  }
  points.value = pointsArray;
};

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

  regeneratePoints();
  const debouncedRegenerate = debounce(regeneratePoints, 200);
  map.getView().on('change:resolution', debouncedRegenerate);
  map.getView().on('change:center', debouncedRegenerate);
});

function buildServiceUrl(resourcePath, outFields = '') {
  const serviceBase = 'https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/';
  const querySuffix = '/FeatureServer/0/query?where=1%3D1&f=json';
  const encodedOutFields = outFields ? `&outFields=${encodeURIComponent(outFields)}` : '';
  return `${serviceBase}${resourcePath}${querySuffix}${encodedOutFields}`;
}

//There is a 2000 feature limit on each API call, so we need to paginate them.
function createPaginatedVectorSource(serviceUrl) {
  return new VectorSource({
    format: esriJson,
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
          hasMore = data?.exceededTransferLimit === true;
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
