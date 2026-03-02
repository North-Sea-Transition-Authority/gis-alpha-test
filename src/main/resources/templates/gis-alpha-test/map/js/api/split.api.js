export async function splitRequest(lines, featureIds) {
  const requestBody = {
    ed50lineCoordinates: lines.map(line => line.ed50Coordinates),
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
