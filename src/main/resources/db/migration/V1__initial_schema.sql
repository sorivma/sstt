create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    name varchar(255) not null,
    created_at timestamp not null default current_timestamp
);

create table subjects (
    id uuid primary key,
    user_id uuid not null references users(id),
    name varchar(255) not null,
    description text,
    semester varchar(120),
    created_at timestamp not null default current_timestamp
);

create table teachers (
    id uuid primary key,
    user_id uuid not null references users(id),
    full_name varchar(255) not null,
    email varchar(255),
    telegram varchar(120),
    notes text,
    created_at timestamp not null default current_timestamp
);

create table source_messages (
    id uuid primary key,
    user_id uuid not null references users(id),
    source_type varchar(40) not null,
    raw_text text not null,
    sender varchar(255),
    received_at timestamp not null,
    processing_status varchar(40) not null,
    created_at timestamp not null default current_timestamp
);

create table tasks (
    id uuid primary key,
    user_id uuid not null references users(id),
    subject_id uuid references subjects(id),
    teacher_id uuid references teachers(id),
    source_message_id uuid references source_messages(id),
    title varchar(255) not null,
    description text,
    type varchar(40) not null,
    status varchar(40) not null,
    priority varchar(40) not null,
    deadline_date date,
    confidence double precision,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index idx_subjects_user_id on subjects(user_id);
create index idx_teachers_user_id on teachers(user_id);
create index idx_source_messages_user_id_created_at on source_messages(user_id, created_at desc);
create index idx_tasks_user_id_deadline_date on tasks(user_id, deadline_date);

insert into users (id, email, name, created_at)
values ('00000000-0000-0000-0000-000000000001', 'demo@sstt.local', 'Demo Student', current_timestamp);
