export async function splitRequest(points, commandJourneyId) {
  const originalSrsCoordinates = [];
  for (let i = 0; i < points.length - 1; i++) {
    originalSrsCoordinates.push([
      [points[i].originalSrsLongitude, points[i].originalSrsLatitude],
      [points[i + 1].originalSrsLongitude, points[i + 1].originalSrsLatitude]
    ]);
  }

  const requestBody = {
    originalSrsCoordinates: originalSrsCoordinates,
    commandJourneyId: commandJourneyId
  };
  console.log(requestBody);
  const response = await fetch("/api/split/execute", {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(requestBody)
  });

  if (!response.ok) {
    throw new Error(`Failed to do split: ${response.statusText}`);
  }

  const responseBody = await response.json();

  return {
    outputFeatureIds: responseBody.outputFeatureIds,
    commandJourneyId: responseBody.commandJourneyId
  };
}

export async function undoSplit(journeyId) {
  const response = await fetch(`/api/split/${journeyId}/undo`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to undo split: ${response.statusText}`);
  }

  const responseBody = await response.json();

  return {
    outputFeatureIds: responseBody.outputFeatureIds,
    commandJourneyId: responseBody.commandJourneyId
  };
}


export async function redoSplit(journeyId) {
  const response = await fetch(`/api/split/${journeyId}/redo`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to redo split: ${response.statusText}`);
  }

  const responseBody = await response.json();

  return {
    outputFeatureIds: responseBody.outputFeatureIds,
    commandJourneyId: responseBody.commandJourneyId
  };
}