import type {Request, Response} from 'express';
import express from 'express';
import * as Terraformer from "@terraformer/arcgis";
import Polyline from "@arcgis/core/geometry/Polyline.js";

const app = express();
const PORT = 8082;

app.use(express.json());

app.post('/api/geo-json-to-esri-json/line', (
    req: Request,
    res: Response
) => {
  try {

    const geoJson = req.body;
    console.log("Received geojson:", req.body);
    const polyline = Polyline.fromJSON(Terraformer.geojsonToArcGIS(geoJson)).toJSON();

    console.log(`EsriJson Polyline: ${JSON.stringify(polyline)}`);
    res.json({
      esriJson: polyline
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Internal Server Error", details: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`REST Server running on http://localhost:${PORT}`);
});