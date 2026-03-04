export async function getSnapPoints(wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat, srsWkid) {
  const params = new URLSearchParams({
    minLon: wgs84MinLon,
    minLat: wgs84MinLat,
    maxLon: wgs84MaxLon,
    maxLat: wgs84MaxLat,
    srsWkid: srsWkid
  });

  const response = await fetch(`/api/snap-points?${params}`);
  const json = await response.json();
  return json.points;
}
