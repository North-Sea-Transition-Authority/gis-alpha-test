import {bngWkid, ed50Wkid} from "./coordinate-system-utils";

const GRID_CONFIGS = {
  [ed50Wkid]: {
    // 1 degree = 3600 arc seconds
    spacingArcSeconds: 30, //must be divisor of arcSecondsPerDegree to match NSTA's quad/bocks
    arcSecondsPerDegree: 3600,
    originLon: 0,  // Grid aligned to whole degrees
    originLat: 0,
  },
  [bngWkid]: {
    spacingMeters: 500,
    originLon: 0,
    originLat: 0,
  }
};

/**
 * Convert coordinate to grid index
 * @param {number} coord - Coordinate in original SRS
 * @param {number} srsWkid - Spatial reference WKID
 * @param {boolean} isLat - true for latitude/northing, false for longitude/easting
 * @returns {number} Integer grid index
 */
export function coordToGridIndex(coord, srsWkid, isLat = false) {
  const config = GRID_CONFIGS[srsWkid];
  if (!config) {
    throw new Error(`Unsupported SRS WKID: ${srsWkid}`);
  }

  if (srsWkid === ed50Wkid) {
    // Convert degrees to arc seconds for exact integer arithmetic
    const arcSeconds = Math.round(coord * config.arcSecondsPerDegree);
    const origin = isLat ? config.originLat : config.originLon;
    const originArcSeconds = origin * config.arcSecondsPerDegree;
    return Math.round((arcSeconds - originArcSeconds) / config.spacingArcSeconds);
  } else if (srsWkid === bngWkid) {
    const origin = isLat ? config.originLat : config.originLon;
    return Math.round((coord - origin) / config.spacingMeters);
  }
}

/**
 * Convert grid index to coordinate
 * @param {number} index - Integer grid index
 * @param {number} srsWkid - Spatial reference WKID
 * @param {boolean} isLat - true for latitude/northing, false for longitude/easting
 * @returns {number} Coordinate in original SRS
 */
export function gridIndexToCoord(index, srsWkid, isLat = false) {
  const config = GRID_CONFIGS[srsWkid];
  if (!config) {
    throw new Error(`Unsupported SRS WKID: ${srsWkid}`);
  }

  if (srsWkid === ed50Wkid) {
    const origin = isLat ? config.originLat : config.originLon;
    const originArcSeconds = origin * config.arcSecondsPerDegree;
    const arcSeconds = originArcSeconds + (index * config.spacingArcSeconds);
    return arcSeconds / config.arcSecondsPerDegree;
  } else if (srsWkid === bngWkid) {
    const origin = isLat ? config.originLat : config.originLon;
    return origin + (index * config.spacingMeters);
  }
}

/**
 * Create grid point ID from indices
 * @param {number} indexX - Longitude/easting index
 * @param {number} indexY - Latitude/northing index
 * @returns {string} Canonical point ID
 */
export function createGridPointId(indexX, indexY) {
  return `${indexX},${indexY}`;
}

/**
 * Check if two grid points are aligned (same row or column)
 * @param {{indexX: number, indexY: number}} pointA
 * @param {{indexX: number, indexY: number}} pointB
 * @returns {boolean}
 */
export function areGridPointsAligned(pointA, pointB) {
  return pointA.indexX === pointB.indexX || pointA.indexY === pointB.indexY;
}