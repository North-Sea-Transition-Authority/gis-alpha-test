CREATE TABLE command_journeys (
    id UUID NOT NULL PRIMARY KEY
);

CREATE TABLE transformation_commands (
    id             UUID NOT NULL PRIMARY KEY,
    command_journey_id UUID NOT NULL,
    status         TEXT NOT NULL,
    type           TEXT NOT NULL,
    command_order  INTEGER NOT NULL,
    input_ids      JSONB NOT NULL,
    CONSTRAINT transformation_commands_journey_fk FOREIGN KEY (command_journey_id) REFERENCES command_journeys(id)
);

CREATE INDEX transformation_commands_command_journey_id_idx ON transformation_commands (command_journey_id);

ALTER TABLE features ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE features ADD COLUMN created_by_command_id UUID;
ALTER TABLE features ADD CONSTRAINT features_created_by_command_fk FOREIGN KEY (created_by_command_id) REFERENCES transformation_commands(id);
CREATE INDEX feature_created_by_command_id_idx ON features (created_by_command_id);