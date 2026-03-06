import proj4 from "proj4";

//definition for ED50 (EPSG:4230) as per https://spatialreference.org/ref/epsg/4230/
proj4.defs('EPSG:4230', '+proj=longlat +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +no_defs');

/**
 * Converts WGS84 (EPSG:4326) coordinates to ED50 (EPSG:4230).
 * @param longitude WGS84 longitude
 * @param latitude WGS84 latitude
 * @returns {[number, number]} ED50 [longitude, latitude]
 */
export function wgs84ToEd50(longitude, latitude) {
  return proj4('EPSG:4326', 'EPSG:4230', [longitude, latitude]);
}

/**
 * Converts ED50 (EPSG:4230) coordinates to WGS84 (EPSG:4326).
 * @param longitude ED50 longitude
 * @param latitude ED50 latitude
 * @returns {[number, number]} WGS84 [longitude, latitude]
 */
export function ed50ToWgs84(longitude, latitude) {
  return proj4('EPSG:4230', 'EPSG:4326', [longitude, latitude]);
}
