create schema if not exists eventstore;

comment on schema eventstore is
    'Append-only event sourcing storage: stream registry, global event log, projection checkpoints, and optional snapshots.';

create table eventstore.global_positions (
    position_name text primary key,
    last_position bigint not null,
    constraint global_positions_name_not_blank check (length(btrim(position_name)) > 0),
    constraint global_positions_last_position_non_negative check (last_position >= 0)
);

comment on table eventstore.global_positions is
    'Singleton-style counters used to assign strict commit-order positions under row-level locks.';
comment on column eventstore.global_positions.position_name is
    'Counter name. The event log uses the events counter.';
comment on column eventstore.global_positions.last_position is
    'Last global position assigned by a committed append transaction.';
comment on constraint global_positions_pkey on eventstore.global_positions is
    'Primary key for named global position counters.';
comment on constraint global_positions_name_not_blank on eventstore.global_positions is
    'Prevents unnamed global position counters.';
comment on constraint global_positions_last_position_non_negative on eventstore.global_positions is
    'Global positions start at zero before the first event is appended.';

insert into eventstore.global_positions (position_name, last_position)
values ('events', 0);

create table eventstore.streams (
    stream_id uuid primary key,
    stream_name text not null unique,
    stream_type text not null,
    current_version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint streams_current_version_non_negative check (current_version >= 0),
    constraint streams_stream_name_not_blank check (length(btrim(stream_name)) > 0),
    constraint streams_stream_type_not_blank check (length(btrim(stream_type)) > 0)
);

comment on table eventstore.streams is
    'Registry of aggregate event streams. One row represents the lifecycle log for one aggregate instance.';
comment on column eventstore.streams.stream_id is
    'Stable technical identifier of the stream, used as the foreign key from event and snapshot rows.';
comment on column eventstore.streams.stream_name is
    'Human-readable unique stream name, usually composed from aggregate type and aggregate id, for example task:<uuid>.';
comment on column eventstore.streams.stream_type is
    'Logical aggregate category such as student, task, subject, material, or ai-extraction.';
comment on column eventstore.streams.current_version is
    'Latest committed stream version. Used for optimistic concurrency checks during append.';
comment on column eventstore.streams.created_at is
    'Timestamp when the stream registry row was created.';
comment on column eventstore.streams.updated_at is
    'Timestamp when the stream version or metadata was last changed.';
comment on constraint streams_pkey on eventstore.streams is
    'Primary key for technical stream identity.';
comment on constraint streams_stream_name_key on eventstore.streams is
    'Ensures every logical stream name maps to exactly one stream row.';
comment on constraint streams_current_version_non_negative on eventstore.streams is
    'Stream version starts at zero before the first event and never becomes negative.';
comment on constraint streams_stream_name_not_blank on eventstore.streams is
    'Prevents empty stream names, because stream names are part of the public event store contract.';
comment on constraint streams_stream_type_not_blank on eventstore.streams is
    'Prevents empty stream types, because projections often filter by stream type.';

create index streams_stream_type_idx
    on eventstore.streams (stream_type);

comment on index eventstore.streams_stream_type_idx is
    'Supports listing or filtering streams by aggregate category.';

create table eventstore.events (
    global_position bigint primary key,
    event_id uuid not null unique,
    stream_id uuid not null references eventstore.streams (stream_id),
    stream_name text not null,
    stream_type text not null,
    stream_version bigint not null,
    event_type text not null,
    event_version integer not null,
    occurred_at timestamptz not null,
    actor_id uuid,
    correlation_id uuid,
    causation_id uuid,
    payload jsonb not null,
    metadata jsonb not null,
    constraint events_stream_version_positive check (stream_version > 0),
    constraint events_event_version_positive check (event_version > 0),
    constraint events_stream_name_not_blank check (length(btrim(stream_name)) > 0),
    constraint events_stream_type_not_blank check (length(btrim(stream_type)) > 0),
    constraint events_event_type_not_blank check (length(btrim(event_type)) > 0),
    constraint events_stream_version_unique unique (stream_id, stream_version)
);

comment on table eventstore.events is
    'Global append-only event log. All domain event types share this table and are ordered by global_position.';
comment on column eventstore.events.global_position is
    'Strict commit-order cursor assigned by locking eventstore.global_positions during append. Projections use it to replay the complete event log.';
comment on column eventstore.events.event_id is
    'Stable event UUID used for external references, correlation, diagnostics, and idempotency checks.';
comment on column eventstore.events.stream_id is
    'Technical stream identifier referencing eventstore.streams.';
comment on column eventstore.events.stream_name is
    'Denormalized stream name captured at append time to make event log queries and diagnostics simpler.';
comment on column eventstore.events.stream_type is
    'Denormalized aggregate category captured at append time for projection filtering and diagnostics.';
comment on column eventstore.events.stream_version is
    'Local sequence number inside one stream. Aggregate reconstruction orders by this value.';
comment on column eventstore.events.event_type is
    'Domain event name in past tense, for example TaskCreated or SourceMessageRecorded.';
comment on column eventstore.events.event_version is
    'Version of the JSON payload contract for this event_type.';
comment on column eventstore.events.occurred_at is
    'Timestamp assigned when the event was appended to the store.';
comment on column eventstore.events.actor_id is
    'Optional identifier of the user or system actor that caused the append.';
comment on column eventstore.events.correlation_id is
    'Optional workflow/request identifier shared by events produced by the same high-level action.';
comment on column eventstore.events.causation_id is
    'Optional event or command identifier that directly caused this event.';
comment on column eventstore.events.payload is
    'Generic JSON domain payload. The event store stores it opaquely; domain modules own its schema.';
comment on column eventstore.events.metadata is
    'Generic JSON technical context such as importer, model, confidence, request details, or diagnostics.';
comment on constraint events_pkey on eventstore.events is
    'Primary key over the global append-log position.';
comment on constraint events_event_id_key on eventstore.events is
    'Ensures each event UUID is unique independently of its global log position.';
comment on constraint events_stream_id_fkey on eventstore.events is
    'Ensures each event belongs to a registered stream.';
comment on constraint events_stream_version_positive on eventstore.events is
    'The first event in a stream has version 1; zero is reserved for an empty stream.';
comment on constraint events_event_version_positive on eventstore.events is
    'Event payload contract versions start at 1.';
comment on constraint events_stream_name_not_blank on eventstore.events is
    'Prevents empty denormalized stream names in the event log.';
comment on constraint events_stream_type_not_blank on eventstore.events is
    'Prevents empty denormalized stream types in the event log.';
comment on constraint events_event_type_not_blank on eventstore.events is
    'Prevents events without a domain event name.';
comment on constraint events_stream_version_unique on eventstore.events is
    'Guarantees exactly one event at each local stream version.';

create index events_stream_idx
    on eventstore.events (stream_id, stream_version);

comment on index eventstore.events_stream_idx is
    'Supports aggregate reconstruction by technical stream id in stream version order.';

create index events_stream_name_idx
    on eventstore.events (stream_name, stream_version);

comment on index eventstore.events_stream_name_idx is
    'Supports aggregate reconstruction and diagnostics by human-readable stream name.';

create index events_stream_type_idx
    on eventstore.events (stream_type, global_position);

comment on index eventstore.events_stream_type_idx is
    'Supports projections or maintenance jobs that process events for one stream type in global order.';

create index events_event_type_idx
    on eventstore.events (event_type);

comment on index eventstore.events_event_type_idx is
    'Supports diagnostics and selective event-type lookups.';

create index events_occurred_at_idx
    on eventstore.events (occurred_at);

comment on index eventstore.events_occurred_at_idx is
    'Supports time-range audits and operational inspection of the event log.';

create index events_correlation_id_idx
    on eventstore.events (correlation_id)
    where correlation_id is not null;

comment on index eventstore.events_correlation_id_idx is
    'Supports tracing all events produced by one workflow or request.';

create table eventstore.projection_offsets (
    projection_name text primary key,
    last_global_position bigint not null,
    updated_at timestamptz not null,
    constraint projection_offsets_name_not_blank check (length(btrim(projection_name)) > 0),
    constraint projection_offsets_position_non_negative check (last_global_position >= 0)
);

comment on table eventstore.projection_offsets is
    'Durable checkpoints for read model projections that consume the global event log.';
comment on column eventstore.projection_offsets.projection_name is
    'Unique projection identifier, for example tasks_projection or academics_projection.';
comment on column eventstore.projection_offsets.last_global_position is
    'Highest global event position fully processed and committed by this projection.';
comment on column eventstore.projection_offsets.updated_at is
    'Timestamp when the projection checkpoint was last saved.';
comment on constraint projection_offsets_pkey on eventstore.projection_offsets is
    'Primary key for one checkpoint row per projection.';
comment on constraint projection_offsets_name_not_blank on eventstore.projection_offsets is
    'Prevents unnamed projection checkpoints.';
comment on constraint projection_offsets_position_non_negative on eventstore.projection_offsets is
    'Projection offsets start at zero before any event is processed.';

create table eventstore.snapshots (
    snapshot_id uuid primary key,
    stream_id uuid not null references eventstore.streams (stream_id),
    stream_name text not null,
    stream_type text not null,
    stream_version bigint not null,
    created_at timestamptz not null,
    payload jsonb not null,
    metadata jsonb not null,
    constraint snapshots_stream_version_positive check (stream_version > 0),
    constraint snapshots_stream_name_not_blank check (length(btrim(stream_name)) > 0),
    constraint snapshots_stream_type_not_blank check (length(btrim(stream_type)) > 0),
    constraint snapshots_stream_version_unique unique (stream_id, stream_version)
);

comment on table eventstore.snapshots is
    'Optional aggregate snapshots used to speed up stream reconstruction for long streams.';
comment on column eventstore.snapshots.snapshot_id is
    'Stable technical identifier of a snapshot record.';
comment on column eventstore.snapshots.stream_id is
    'Technical stream identifier referencing eventstore.streams.';
comment on column eventstore.snapshots.stream_name is
    'Denormalized stream name for diagnostics and easier snapshot lookup.';
comment on column eventstore.snapshots.stream_type is
    'Denormalized aggregate category for diagnostics and maintenance tasks.';
comment on column eventstore.snapshots.stream_version is
    'Stream version represented by this snapshot payload.';
comment on column eventstore.snapshots.created_at is
    'Timestamp when the snapshot was created.';
comment on column eventstore.snapshots.payload is
    'Generic JSON aggregate state snapshot at stream_version.';
comment on column eventstore.snapshots.metadata is
    'Generic JSON technical context about snapshot creation.';
comment on constraint snapshots_pkey on eventstore.snapshots is
    'Primary key for technical snapshot identity.';
comment on constraint snapshots_stream_id_fkey on eventstore.snapshots is
    'Ensures each snapshot belongs to a registered stream.';
comment on constraint snapshots_stream_version_positive on eventstore.snapshots is
    'Snapshots can only represent committed stream versions.';
comment on constraint snapshots_stream_name_not_blank on eventstore.snapshots is
    'Prevents empty denormalized stream names in snapshot rows.';
comment on constraint snapshots_stream_type_not_blank on eventstore.snapshots is
    'Prevents empty denormalized stream types in snapshot rows.';
comment on constraint snapshots_stream_version_unique on eventstore.snapshots is
    'Allows at most one snapshot for a stream at a given stream version.';

create index snapshots_stream_idx
    on eventstore.snapshots (stream_id, stream_version desc);

comment on index eventstore.snapshots_stream_idx is
    'Supports loading the latest snapshot for a stream before replaying remaining events.';
