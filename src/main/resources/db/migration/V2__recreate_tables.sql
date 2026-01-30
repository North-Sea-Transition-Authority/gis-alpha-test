DROP TABLE points;
DROP TABLE lines;
DROP TABLE polygons;
DROP TABLE features;

CREATE TABLE features (
    id UUID NOT NULL
    , type TEXT NOT NULL
    , srs INTEGER NOT NULL
    , shape_sid_id NUMERIC
    , feature_name TEXT
    , feature_area NUMERIC
    , parent_feature_id UUID
    , test_case TEXT
    , CONSTRAINT features_pk PRIMARY KEY (id)
);
ALTER TABLE features ADD CONSTRAINT features_parent_feature_id_fk FOREIGN KEY (parent_feature_id) REFERENCES features(id);
ALTER TABLE features ADD CONSTRAINT features_shape_sid_id_test_case_unique UNIQUE (shape_sid_id, test_case);


CREATE TABLE polygons (
    id UUID NOT NULL
    , oracle_polygon_ssid NUMERIC
    , feature_id UUID NOT NULL
    , attributes JSONB NOT NULL
    , CONSTRAINT polygons_pk PRIMARY KEY (id)
    , CONSTRAINT polygons_feature_id_fk FOREIGN KEY (feature_id) REFERENCES features (id)
);

CREATE INDEX polygons_feature_id_idx ON polygons (feature_id);

CREATE TABLE lines (
    id UUID NOT NULL
    , oracle_line_ssid NUMERIC
    , polygon_id UUID
    , navigation_type TEXT NOT NULL
    , ring_number INTEGER NOT NULL
    , ring_connection_order INTEGER NOT NULL
    , attributes JSONB NOT NULL
    , line_json TEXT
    , boundary_sid_id NUMERIC
    , CONSTRAINT lines_pk PRIMARY KEY (id)
    , CONSTRAINT lines_polygon_id_fk FOREIGN KEY (polygon_id) REFERENCES polygons (id)
);

CREATE INDEX lines_polygon_id_idx ON lines (polygon_id);