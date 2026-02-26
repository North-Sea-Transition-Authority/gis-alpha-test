import {EsriJSON} from "ol/format";
import VectorSource from "ol/source/Vector";

const esriJson = new EsriJSON();

export function buildServiceUrl(resourcePath, outFields = '') {
  const serviceBase = 'https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/';
  const querySuffix = '/FeatureServer/0/query?where=1%3D1&f=json';
  const encodedOutFields = outFields ? `&outFields=${encodeURIComponent(outFields)}` : '';
  return `${serviceBase}${resourcePath}${querySuffix}${encodedOutFields}`;
}

//There is a 2000 feature limit on each API call, so we need to paginate them.
export function createPaginatedVectorSource(serviceUrl) {
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