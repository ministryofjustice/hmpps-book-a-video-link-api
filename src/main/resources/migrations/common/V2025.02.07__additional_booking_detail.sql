CREATE TABLE additional_booking_detail
(
  additional_booking_detail_id  bigserial NOT NULL CONSTRAINT additional_booking_detail_pk PRIMARY KEY,
  video_booking_id              bigint NOT NULL REFERENCES video_booking(video_booking_id),
  contact_name                  varchar(100) NOT NULL,
  contact_email                 varchar(100),
  contact_number                varchar(30),
  extra_information             varchar(100)
);

CREATE UNIQUE INDEX idx_video_booking_id ON additional_booking_detail(video_booking_id);
