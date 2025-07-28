CREATE TABLE features (
  id UUID NOT NULL
, type TEXT NOT NULL
, srs INTEGER NOT NULL
, CONSTRAINT features_pk PRIMARY KEY (id)
);

CREATE TABLE polygons (
  id UUID NOT NULL
, feature_id UUID NOT NULL
, attributes JSONB NOT NULL
, CONSTRAINT polygons_pk PRIMARY KEY (id)
, CONSTRAINT polygons_feature_id_fk FOREIGN KEY (feature_id) REFERENCES features (id)
);

CREATE INDEX polygons_feature_id_idx ON polygons (feature_id);

CREATE TABLE lines (
  id UUID NOT NULL
, feature_id UUID NOT NULL
, polygon_id UUID
, navigation_type TEXT NOT NULL
, exterior_ring BOOLEAN NOT NULL
, attributes JSONB NOT NULL
, CONSTRAINT lines_pk PRIMARY KEY (id)
, CONSTRAINT lines_feature_id_fk FOREIGN KEY (feature_id) REFERENCES features (id)
, CONSTRAINT lines_polygon_id_fk FOREIGN KEY (polygon_id) REFERENCES polygons (id)
);

CREATE INDEX lines_feature_id_idx ON lines (feature_id);
CREATE INDEX lines_polygon_id_idx ON lines (polygon_id);

CREATE TABLE points (
  id UUID NOT NULL
, feature_id UUID NOT NULL
, line_id UUID
, x NUMERIC NOT NULL
, z NUMERIC NOT NULL
, CONSTRAINT points_pk PRIMARY KEY (id)
, CONSTRAINT points_feature_id_fk FOREIGN KEY (feature_id) REFERENCES features (id)
, CONSTRAINT points_line_id_fk FOREIGN KEY (line_id) REFERENCES lines (id)
);

CREATE INDEX points_feature_id_idx ON points (feature_id);
CREATE INDEX points_line_id_idx ON points (line_id);
