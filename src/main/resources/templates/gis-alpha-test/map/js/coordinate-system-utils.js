import proj4 from "proj4";

//ED50 (wkid:4230) as per https://epsg.io/4230
proj4.defs('EPSG:4230', '+proj=longlat +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +no_defs');
//British national grid (wkid: 27700) as per https://epsg.io/27700
proj4.defs('EPSG:27700', '+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 +units=m +no_defs');

export const ed50Wkid = 4230; //ED50
export const bngWkid = 27700; //British National Grid
export const wgs84Wkid = 4326; //World Geodetic System 84

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
 * Converts WGS84 (EPSG:4326) coordinates to British National Grid (EPSG:27700).
 * @param longitude WGS84 longitude
 * @param latitude WGS84 latitude
 * @returns {[number, number]} BNG [easting, northing]
 */
export function wgs84ToBng(longitude, latitude) {
  return proj4('EPSG:4326', 'EPSG:27700', [longitude, latitude]);
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

/**
 * Converts British National Grid (EPSG:27700) coordinates to WGS84 (EPSG:4326).
 * @param easting BNG easting
 * @param northing BNG northing
 * @returns {[number, number]} WGS84 [longitude, latitude]
 */
export function bngToWgs84(easting, northing) {
  return proj4('EPSG:27700', 'EPSG:4326', [easting, northing]);
}

/**
 * Converts Decimal Degrees to Degrees, Minutes, Seconds (DMS)
 * @param {Number|String} dd - The decimal degree coordinate
 * @param {Boolean} isLat - True if the coordinate is Latitude (Y axis)
 * @returns {Object} { d: Number, m: Number, s: Number, hemi: String }
 */
export function toDMS(dd, isLat) {
  const val = parseFloat(dd) || 0;
  const absVal = Math.abs(val);

  let degrees = Math.trunc(absVal);
  let minutes = Math.trunc((absVal - degrees) * 60);
  let seconds = Number((((absVal - degrees) * 60 - minutes) * 60).toFixed(3));

  let hemi = '';
  if (isLat) {
    hemi = val < 0 || Object.is(val, -0) ? 'S' : 'N';
  } else {
    hemi = val < 0 || Object.is(val, -0) ? 'W' : 'E';
  }

  if (seconds === 60) {
    //59.9996 will round to 60
    seconds = 0;
    minutes += 1;
  }

  if (minutes === 60) {
    minutes = 0;
    degrees += 1;
  }

  return { d: degrees, m: minutes, s: seconds, hemi };
}

/**
 * @param {Object} dms - { d: Number, m: Number, s: Number, hemi: String }
 * @returns {String} DMS formatted string
 */
export function dmsToString(dms) {
  return `${dms.d}° ${dms.m}' ${dms.s}" ${dms.hemi}`;
}

/**
 * Converts Degrees, Minutes, Seconds (DMS) back to Decimal Degrees
 * @param {Number|String} d - Degrees
 * @param {Number|String} m - Minutes
 * @param {Number|String} s - Seconds
 * @param {String} hemi - Hemisphere ('N', 'S', 'E', 'W')
 * @returns {Number} Decimal degrees
 */
export function toDecimalDegrees(d, m, s, hemi) {
  const parsedD = Math.abs(parseFloat(d)) || 0;
  const parsedM = Math.abs(parseFloat(m)) || 0;
  const parsedS = Math.abs(parseFloat(s)) || 0;

  let dd = parsedD + (parsedM / 60) + (parsedS / 3600);

  if (hemi === 'S' || hemi === 'W') {
    dd = -dd;
  }
  return dd;
}