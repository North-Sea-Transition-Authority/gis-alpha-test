export async function splitRequest(points, featureIds) {
  const originalSrsCoordinates = [];
  for (let i = 0; i < points.length - 1; i++) {
    originalSrsCoordinates.push([
      [points[i].originalSrsLongitude, points[i].originalSrsLatitude],
      [points[i + 1].originalSrsLongitude, points[i + 1].originalSrsLatitude]
    ]);
  }

  const requestBody = {
    originalSrsCoordinates: originalSrsCoordinates,
    featureIds: featureIds.split(',')
  };
  console.log(requestBody);
  const outputFeatureIds = [];

  const response = await fetch("/api/split", {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(requestBody)
  });

  await response.json()
    .then(featureIds => {
      for (let featureId of featureIds) {
        outputFeatureIds.push(featureId);
      }
    })
    .catch(console.error);

  return outputFeatureIds;
}
