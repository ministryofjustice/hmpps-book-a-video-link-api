--
-- TABLES
--

---------------------------------------------------------------------------------------
-- Contains coded reference values, used to constrain the values of lists/validation.
-- e.g. Booking types, meeting types, hearing types, status etc..
----------------------------------------------------------------------------------------

CREATE TABLE reference_code
(
    reference_code_id   bigserial NOT NULL CONSTRAINT reference_code_pk PRIMARY KEY,
    group_code          varchar(40) NOT NULL,  
    code                varchar(40) NOT NULL,
    description         varchar(100) NOT NULL,
    created_by          varchar(100) NOT NULL,
    created_time        timestamp NOT NULL,
    amended_by          varchar(100),
    amended_time        timestamp           
);

CREATE UNIQUE INDEX idx_reference_code_group ON reference_code(group_code, code);

---------------------------------------------------------------------------------------
-- These are the courts that are known to the service.
-- A court has contacts who will be notified of bookings and changes.
-- A court has users who have selected to view/add bookings for it.
-- A court has video_bookings related to it.
----------------------------------------------------------------------------------------

CREATE TABLE court
(
    court_id            bigserial NOT NULL CONSTRAINT court_id_pk PRIMARY KEY,
    code                varchar(30) NOT NULL UNIQUE,
    description         varchar(100) NOT NULL,
    enabled             boolean NOT NULL,
    notes               varchar(200),
    created_by          varchar(100) NOT NULL,
    created_time        timestamp NOT NULL,
    amended_by          varchar(100),
    amended_time        timestamp 
);

CREATE INDEX idx_court_code ON court(code);

---------------------------------------------------------------------------------------
-- The people involved in video link bookings at each court.
-- This can be a single group email contact or a list of individual contacts.
-- This list of contacts will be notified of bookings / changes for this court.
----------------------------------------------------------------------------------------

CREATE TABLE court_contact
(
    court_contact_id    bigserial NOT NULL CONSTRAINT court_contact_id_pk PRIMARY KEY,
    court_id            bigint NOT NULL REFERENCES court(court_id),
    name                varchar(100) NOT NULL,
    email               varchar(100),
    telephone           varchar(20),
    position            varchar(100),
    enabled             boolean NOT NULL,
    notes               varchar(200),
    created_by          varchar(100) NOT NULL,
    created_time        timestamp NOT NULL,
    amended_by          varchar(100),
    amended_time        timestamp
);

CREATE INDEX idx_court_court_id ON court_contact(court_id);
CREATE INDEX idx_court_contact_name ON court_contact(name);
CREATE INDEX idx_court_contact_email ON court_contact(email);


---------------------------------------------------------------------------------------
-- The probation teams involved in video link bookings.
-- A probation team has contacts who will be notified of bookings, and changes to them.
-- A probation team has users who have selected to view/add bookings for it.
-- A probation team has video bookings related to it.
----------------------------------------------------------------------------------------

CREATE TABLE probation_team
(
    probation_team_id    bigserial NOT NULL CONSTRAINT probation_team_id_pk PRIMARY KEY,
    code                 varchar(40) NOT NULL UNIQUE,
    description          varchar(100) NOT NULL,
    enabled              boolean NOT NULL,
    notes                varchar(100),
    created_by           varchar(100) NOT NULL,
    created_time         timestamp NOT NULL,
    amended_by           varchar(100),
    amended_time         timestamp
);

CREATE INDEX idx_probation_team_code ON probation_team(code);

---------------------------------------------------------------------------------------
-- The people involved in video link bookings for a probation team.
-- This can be a single group email contact or a list of individual contacts.
-- This list of contacts will be notified of bookings for this team, and any changes.
----------------------------------------------------------------------------------------

CREATE TABLE probation_team_contact
(
    probation_team_contact_id bigserial NOT NULL CONSTRAINT probation_team_contact_id_pk PRIMARY KEY,
    probation_team_id    bigint NOT NULL REFERENCES probation_team(probation_team_id),
    name                 varchar(100) NOT NULL,
    email                varchar(100),
    telephone            varchar(20),
    position             varchar(100),
    enabled              boolean NOT NULL,
    notes                varchar(200),
    created_by           varchar(100) NOT NULL,
    created_time         timestamp NOT NULL,
    amended_by           varchar(100),
    amended_time         timestamp
);

CREATE INDEX idx_probation_contact_team_id ON probation_team_contact(probation_team_id);
CREATE INDEX idx_probation_contact_name ON probation_team_contact(name);
CREATE INDEX idx_probation_contact_email ON probation_team_contact(email);

---------------------------------------------------------------------------------------
-- Reference table for prisons - populate from register
---------------------------------------------------------------------------------------

CREATE TABLE prison
(
    prison_id    bigserial   NOT NULL CONSTRAINT prison_id_pk PRIMARY KEY,
    code         varchar(10) NOT NULL UNIQUE,
    name         varchar(60) NOT NULL,
    enabled      boolean NOT NULL,
    notes        varchar(200),
    created_by   varchar(100) NOT NULL,
    created_time timestamp NOT NULL,
    amended_by   varchar(100),
    amended_time timestamp
);

CREATE UNIQUE INDEX idx_prison_code ON prison(code);

---------------------------------------------------------------------------------------
-- Contacts for this prison
---------------------------------------------------------------------------------------

CREATE TABLE prison_contact
(
    prison_contact_id  bigserial NOT NULL CONSTRAINT prison_contact_id_pk PRIMARY KEY,
    prison_id bigint NOT NULL REFERENCES prison(prison_id),
    name varchar(100) NOT NULL,
    email varchar(100),
    telephone varchar(20),
    position varchar(100),
    enabled boolean NOT NULL,
    notes varchar(200),
    created_by   varchar(100) NOT NULL,
    created_time timestamp NOT NULL,
    amended_by   varchar(100),
    amended_time timestamp
);

CREATE INDEX idx_prison_prison_id ON prison_contact(prison_id);
CREATE INDEX idx_prison_contact_name ON prison_contact(name);
CREATE INDEX idx_prison_contact_email ON prison_contact(email);

---------------------------------------------------------------------------------------
-- This is the main table for a video booking.
-- It has a type - PROBATION or COURT booking (can extend to others)
-- court_id / hearing_type / probation_team_id / probation_meeting_type  
--     - are nullable FK references
----------------------------------------------------------------------------------------

CREATE TABLE video_booking
(
    video_booking_id       bigserial NOT NULL CONSTRAINT video_booking_pk PRIMARY KEY,
    booking_type           varchar(40) NOT NULL,  -- COURT, PROBATION
    status_code            varchar(20) NOT NULL,  -- ACTIVE, COMPLETED, EXPIRED, CANCELLED
    court_id               bigint REFERENCES court(court_id), -- Nullable
    hearing_type           varchar(40), -- Nullable
    probation_team_id      bigint REFERENCES probation_team(probation_team_id), -- Nullable
    probation_meeting_type varchar(40), -- Nullable
    video_url              varchar(120),
    comments               varchar(400),
    created_by             varchar(100) NOT NULL, 
    created_time           timestamp    NOT NULL,
    amended_by             varchar(100), 
    amended_time           timestamp
);

CREATE INDEX idx_video_booking_type ON video_booking(booking_type);
CREATE INDEX idx_video_booking_status_code ON video_booking(status_code);
CREATE INDEX idx_video_booking_court_id ON video_booking(court_id);
CREATE INDEX idx_video_booking_probation_team_id ON video_booking(probation_team_id);
CREATE INDEX idx_video_booking_hearing_type ON video_booking(hearing_type);
CREATE INDEX idx_video_booking_meeting_type ON video_booking(probation_meeting_type);

---------------------------------------------------------------------------------------
-- This is the prison appointments related to the video booking
-- Probation will have 1 - RR or PSR
-- Courts 1-3  - PRE_CONFERENCE, HEARING, POST_CONFERENCE per person.
----------------------------------------------------------------------------------------

CREATE TABLE prison_appointment
(
    prison_appointment_id  bigserial NOT NULL CONSTRAINT prison_appointment_pk PRIMARY KEY,
    video_booking_id       bigint NOT NULL REFERENCES video_booking(video_booking_id), 
    prison_code            varchar(5) NOT NULL REFERENCES prison(code),
    prisoner_number        varchar(7) NOT NULL,  -- NOMS number
    appointment_type       varchar(40) NOT NULL,
    comments               varchar(200),
    prison_loc_key         varchar(160) NOT NULL,  -- from locations API
    appointment_date       date NOT NULL,
    start_time             time without time zone NOT NULL,
    end_time               time without time zone NOT NULL,
    created_by             varchar(100) NOT NULL,
    created_time           timestamp    NOT NULL,
    amended_by             varchar(100), 
    amended_time           timestamp
);

CREATE INDEX idx_prison_appointment_video_booking_id ON prison_appointment(video_booking_id);
CREATE INDEX idx_prison_appointment_prison_code ON prison_appointment(prison_code);
CREATE INDEX idx_prison_appointment_prisoner_number ON prison_appointment(prisoner_number);
CREATE INDEX idx_prison_appointment_loc_key ON prison_appointment(prison_loc_key);
CREATE INDEX idx_prison_appointment_date ON prison_appointment(appointment_date);
CREATE INDEX idx_prison_appointment_start_time ON prison_appointment(start_time);
CREATE INDEX idx_prison_appointment_end_time ON prison_appointment(end_time);
CREATE UNIQUE INDEX idx_prison_appointment_loc_date_start_end on prison_appointment(prison_loc_key, appointment_date, start_time, end_time);

---------------------------------------------------------------------------------------
-- This is the third party contact details for attendees to a booking
-- For interpreters, legal, probation officer, keyworkers etc
---------------------------------------------------------------------------------------

CREATE TABLE third_party 
(
    third_party_id     bigserial NOT NULL CONSTRAINT third_party_id_pk PRIMARY KEY,
    video_booking_id   bigint NOT NULL REFERENCES video_booking(video_booking_id), 
    third_party_type   varchar(40) NOT NULL,
    name               varchar(100),
    organisation       varchar(100),
    email              varchar(100),
    telephone          varchar(20),
    created_by         varchar(100) NOT NULL, 
    created_time       timestamp NOT NULL,
    amended_by         varchar(100), 
    amended_time       timestamp
);

CREATE INDEX idx_third_party_video_booking_id ON third_party(video_booking_id);

---------------------------------------------------------------------------------------
-- This is the court / user prefence - which courts to view/book for 
---------------------------------------------------------------------------------------

CREATE TABLE user_court
(
    user_court_id   bigserial   NOT NULL CONSTRAINT user_court_id_pk PRIMARY KEY,
    court_id        bigint NOT NULL REFERENCES court(court_id),
    username        varchar(100) NOT NULL,
    created_by      varchar(100) NOT NULL,         
    created_time    timestamp    NOT NULL
);

CREATE INDEX idx_user_court_court_id ON user_court(court_id);
CREATE INDEX idx_user_court_username ON user_court(username);

---------------------------------------------------------------------------------------
-- This is the probation user and team preference - which teams to view/book for
---------------------------------------------------------------------------------------

CREATE TABLE user_probation
(
    user_probation_id  bigserial   NOT NULL CONSTRAINT user_probation_id_pk PRIMARY KEY,
    probation_team_id  bigint NOT NULL REFERENCES probation_team(probation_team_id),
    username           varchar(100) NOT NULL,
    created_by         varchar(100) NOT NULL,
    created_time       timestamp    NOT NULL
);

CREATE INDEX idx_user_probation_team_id ON user_probation(probation_team_id);
CREATE INDEX idx_user_probation_username ON user_probation(username);

---------------------------------------------------------------------------------------
-- This is the record of notifications sent about bookings
-- Create, update, cancel - sent to prison contacts, court/probation contacts, 
-- and to any third parties for the booking, and to the creator themselves
---------------------------------------------------------------------------------------

CREATE TABLE notification
(
    notification_id            bigserial  NOT NULL CONSTRAINT notification_id_pk PRIMARY KEY,
    video_booking_id           bigint NOT NULL REFERENCES video_booking(video_booking_id),
    template_name              varchar(100),
    email                      varchar(100),
    reason                     varchar(40) NOT NULL,
    gov_notify_notification_id uuid NOT NULL,
    created_time               timestamp NOT NULL
);

CREATE INDEX idx_notification_booking_id ON notification(video_booking_id);
CREATE INDEX idx_notification_email ON notification(email);
CREATE UNIQUE INDEX idx_gov_notify_notification_id ON notification(gov_notify_notification_id);

---------------------------------------------------------------------------------------
-- This is the history of changes to a booking
-- Create, update, or cancel
---------------------------------------------------------------------------------------

CREATE TABLE booking_history 
(
    booking_history_id  bigserial  NOT NULL CONSTRAINT booking_history_id_pk PRIMARY KEY,
    video_booking_id    bigint NOT NULL REFERENCES video_booking(video_booking_id),
    history_type         varchar(40) NOT NULL,
    change_detail       varchar(100),
    created_by          varchar(100) NOT NULL,         
    created_time        timestamp    NOT NULL
);

CREATE INDEX idx_history_video_booking_id ON booking_history(video_booking_id);
CREATE INDEX idx_history_type ON booking_history(history_type);

-- NOTES
-- To follow:
--    - prison room decorations (CVP or PCVL links, capacity, usage, status, schedule, message)
--    - court room decorations (CVP links, message)

-- END --
