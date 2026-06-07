create schema if not exists projections;

create table projections.subjects (
    subject_id uuid primary key,
    name text not null,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint subjects_name_not_blank check (length(btrim(name)) > 0),
    constraint subjects_stream_version_positive check (stream_version > 0)
);

create table projections.teachers (
    teacher_id uuid primary key,
    full_name text not null,
    email text,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint teachers_full_name_not_blank check (length(btrim(full_name)) > 0),
    constraint teachers_email_not_blank check (email is null or length(btrim(email)) > 0),
    constraint teachers_stream_version_positive check (stream_version > 0)
);

create table projections.semesters (
    semester_id uuid primary key,
    name text not null,
    starts_on date,
    ends_on date,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint semesters_name_not_blank check (length(btrim(name)) > 0),
    constraint semesters_dates_order check (starts_on is null or ends_on is null or ends_on >= starts_on),
    constraint semesters_stream_version_positive check (stream_version > 0)
);

create index subjects_name_idx on projections.subjects (name);
create index teachers_full_name_idx on projections.teachers (full_name);
create index semesters_dates_idx on projections.semesters (starts_on, ends_on);

create table projections.task_statuses (
    status_id uuid primary key,
    student_id uuid not null,
    status_key text not null,
    title text not null,
    icon text not null,
    sort_order integer not null,
    terminal boolean not null,
    active boolean not null,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint task_statuses_status_key_format check (status_key ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    constraint task_statuses_title_not_blank check (length(btrim(title)) > 0),
    constraint task_statuses_icon_not_blank check (length(btrim(icon)) > 0),
    constraint task_statuses_stream_version_positive check (stream_version > 0),
    constraint task_statuses_student_key_unique unique (student_id, status_key)
);

create table projections.tasks (
    task_id uuid primary key,
    student_id uuid not null,
    title text not null,
    description text,
    subject_id uuid,
    teacher_id uuid,
    semester_id uuid,
    status_key text not null,
    priority text not null,
    deadline timestamptz,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint tasks_title_not_blank check (length(btrim(title)) > 0),
    constraint tasks_status_key_format check (status_key ~ '^[a-z0-9][a-z0-9-]{0,63}$'),
    constraint tasks_priority_known check (priority in ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    constraint tasks_stream_version_positive check (stream_version > 0)
);

create index task_statuses_student_order_idx on projections.task_statuses (student_id, active, sort_order);
create index tasks_student_status_idx on projections.tasks (student_id, status_key, deadline);
create index tasks_subject_idx on projections.tasks (subject_id) where subject_id is not null;
create index tasks_teacher_idx on projections.tasks (teacher_id) where teacher_id is not null;

create table projections.source_messages (
    message_id uuid primary key,
    student_id uuid not null,
    source_type text not null,
    content text not null,
    status text not null,
    processing_summary text,
    failure_reason text,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint source_messages_source_type_not_blank check (length(btrim(source_type)) > 0),
    constraint source_messages_content_not_blank check (length(btrim(content)) > 0),
    constraint source_messages_status_known check (status in ('NEW', 'PROCESSED', 'FAILED')),
    constraint source_messages_stream_version_positive check (stream_version > 0)
);

create index source_messages_student_status_idx on projections.source_messages (student_id, status, created_at desc);

create table projections.materials (
    material_id uuid primary key,
    student_id uuid not null,
    file_name text not null,
    content_type text,
    storage_key text not null,
    summary text,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint materials_file_name_not_blank check (length(btrim(file_name)) > 0),
    constraint materials_storage_key_not_blank check (length(btrim(storage_key)) > 0),
    constraint materials_stream_version_positive check (stream_version > 0)
);

create index materials_student_created_idx on projections.materials (student_id, created_at desc);

create table projections.relations (
    relation_id uuid primary key,
    student_id uuid not null,
    from_entity_type text not null,
    from_entity_id uuid not null,
    relation_type text not null,
    to_entity_type text not null,
    to_entity_id uuid not null,
    active boolean not null,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint relations_from_entity_type_not_blank check (length(btrim(from_entity_type)) > 0),
    constraint relations_relation_type_not_blank check (length(btrim(relation_type)) > 0),
    constraint relations_to_entity_type_not_blank check (length(btrim(to_entity_type)) > 0),
    constraint relations_stream_version_positive check (stream_version > 0)
);

create index relations_from_entity_idx on projections.relations (student_id, from_entity_type, from_entity_id) where active = true;
create index relations_to_entity_idx on projections.relations (student_id, to_entity_type, to_entity_id) where active = true;

create table projections.chat_threads (
    thread_id uuid primary key,
    student_id uuid not null,
    title text not null,
    last_message_at timestamptz,
    stream_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint chat_threads_title_not_blank check (length(btrim(title)) > 0),
    constraint chat_threads_stream_version_positive check (stream_version > 0)
);

create table projections.chat_messages (
    message_id uuid primary key,
    thread_id uuid not null references projections.chat_threads (thread_id),
    role text not null,
    content text not null,
    created_at timestamptz not null,
    constraint chat_messages_role_known check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    constraint chat_messages_content_not_blank check (length(btrim(content)) > 0)
);

create index chat_threads_student_recent_idx on projections.chat_threads (student_id, coalesce(last_message_at, created_at) desc);
create index chat_messages_thread_created_idx on projections.chat_messages (thread_id, created_at);
