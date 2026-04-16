ALTER TABLE features ADD COLUMN command_journey_id UUID;

ALTER TABLE features
ADD CONSTRAINT features_command_journey_fk
FOREIGN KEY (command_journey_id)
REFERENCES command_journeys(id);

CREATE INDEX feature_command_journey_id_idx ON features(command_journey_id);