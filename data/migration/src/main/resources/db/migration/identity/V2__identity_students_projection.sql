create schema if not exists identity;

comment on schema identity is
    'Read models and storage projections for student identity data.';

create table identity.students (
    student_id uuid primary key,
    full_name text not null,
    email text not null,
    group_name text not null,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint students_full_name_not_blank check (length(btrim(full_name)) > 0),
    constraint students_email_not_blank check (length(btrim(email)) > 0),
    constraint students_group_name_not_blank check (length(btrim(group_name)) > 0),
    constraint students_stream_version_positive check (stream_version > 0)
);

comment on table identity.students is
    'Current student profile read model rebuilt from eventstore student streams.';
comment on column identity.students.student_id is
    'Stable student identifier from the student event stream.';
comment on column identity.students.full_name is
    'Current student full name.';
comment on column identity.students.email is
    'Current student email address.';
comment on column identity.students.group_name is
    'Current academic group name.';
comment on column identity.students.stream_version is
    'Latest student stream version applied to this projection row.';
comment on column identity.students.created_at is
    'Timestamp of the event that first created this projection row.';
comment on column identity.students.updated_at is
    'Timestamp when this projection row was last updated.';
comment on constraint students_pkey on identity.students is
    'Primary key for one current profile row per student.';
comment on constraint students_full_name_not_blank on identity.students is
    'Prevents blank full names in the student read model.';
comment on constraint students_email_not_blank on identity.students is
    'Prevents blank email values in the student read model.';
comment on constraint students_group_name_not_blank on identity.students is
    'Prevents blank group names in the student read model.';
comment on constraint students_stream_version_positive on identity.students is
    'Projection rows must represent an applied event from a positive stream version.';

create index students_group_name_idx
    on identity.students (group_name, full_name);

comment on index identity.students_group_name_idx is
    'Supports listing students by academic group and name.';
