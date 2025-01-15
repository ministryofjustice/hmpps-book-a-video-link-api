-- -------------------------------------------------------------------------------------
-- These are the tables which hold additional location attributes, used to "decorate" the
-- locations retrieved from the internal locations API with extra data that can be used
-- to determine who can book into it, and when.
-- ----------------------------------------------------------------------------------------

CREATE TABLE location_attribute
(
    location_attribute_id  bigserial NOT NULL CONSTRAINT location_attribute_pk PRIMARY KEY,
    location_key           uuid NOT NULL,  -- from locations API unique key (uuid)
    prison_id              bigint NOT NULL REFERENCES prison(prison_id),
    location_status        varchar(20) NOT NULL, -- ACTIVE or INACTIVE
    status_message         varchar(100), -- Description for inactive rooms and why
    expected_active_date   date, -- Date we expect the room to become active again
    location_usage         varchar(20) NOT NULL,  -- COURT, PROBATION, SHARED, or SCHEDULE
    allowed_parties        varchar(200), -- Comma-separated values of court or probation codes
    prison_video_url       varchar(120),  -- Video URL specific for this room
    notes                  varchar(400),  -- Notes about the rules or link for this room
    created_by             varchar(100) NOT NULL,
    created_time           timestamp NOT NULL,
    amended_by             varchar(100),
    amended_time           timestamp
);

CREATE UNIQUE INDEX idx_location_attribute_location_key ON location_attribute(location_key);
CREATE INDEX idx_location_attribute_prison_id ON location_attribute(prison_id);

-- -------------------------------------------------------------------------------------
-- This table holds the rules when a particular room is defined with locationUsage of
-- SCHEDULED, and indicates that it can support different types of bookings at different
-- times of the day or week. Each row defines one time period, and by default, if any
-- time period is not described, the room will revert to a SHARED room i.e. any one can
-- book into it. There could be many rows (rules) for each location, but cannot overlap times.
-- ----------------------------------------------------------------------------------------

CREATE TABLE location_schedule
(
    location_schedule_id  bigserial NOT NULL CONSTRAINT location_schedule_pk PRIMARY KEY,
    location_attribute_id bigint NOT NULL REFERENCES location_attribute(location_attribute_id),
    start_day_of_week     varchar(12), -- Day of the week
    end_day_of_week       varchar(12), -- Day of the week
    start_time            time without time zone NOT NULL,
    end_time              time without time zone NOT NULL,
    location_usage        varchar(20) NOT NULL,  -- COURT, PROBATION, or SHARED
    allowed_parties       varchar(200), -- Comma-separated values of court or probation codes
    notes                 varchar(400), -- Notes about this rule
    created_by            varchar(100) NOT NULL,
    created_time          timestamp NOT NULL,
    amended_by            varchar(100),
    amended_time          timestamp
);

CREATE UNIQUE INDEX idx_location_schedule_attribute_id ON location_schedule(location_attribute_id);
