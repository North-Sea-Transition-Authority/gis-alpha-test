export async function splitRequest(lines, featureIds) {
  const requestBody = {
    ed50lineCoordinates: lines.map(line => line.ed50Coordinates),
    featureIds: featureIds.split(',')
  };
  console.log(requestBody);
  const response = await fetch("/api/split", {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(requestBody)
  });
  const status = response.status;
  console.log(status);
}