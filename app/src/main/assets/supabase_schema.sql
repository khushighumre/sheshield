-- SheShield Supabase PostgreSQL Schema & RLS Policies
-- Phase 2 Production Schema

-- 1. Incidents Table
create table if not exists public.incidents (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade,
    device_id text not null,
    incident_type text not null,
    started_at bigint not null,
    ended_at bigint,
    status text not null default 'ACTIVE',
    notes text,
    is_remote_alert boolean not null default false,
    created_at timestamptz default now()
);

-- 2. Incident Points (GPS track) Table
create table if not exists public.incident_points (
    id bigserial primary key,
    incident_id uuid references public.incidents(id) on delete cascade,
    lat double precision,
    lng double precision,
    recorded_at bigint not null
);

-- 3. P2P / Mesh Messages Table
create table if not exists public.p2p_messages (
    message_id uuid primary key,
    incident_id uuid not null,
    sender_id text not null,
    synced_by_user_id uuid references auth.users(id) on delete set null,
    message_type text not null, -- SOS, LOCATION_UPDATE, SAFE
    message_text text not null,
    lat double precision,
    lng double precision,
    wall_time bigint not null,
    counter integer not null,
    hop_count integer not null,
    is_forwarded boolean not null default false,
    created_at timestamptz default now()
);

-- Enable Row Level Security (RLS)
alter table public.incidents enable row level security;
alter table public.incident_points enable row level security;
alter table public.p2p_messages enable row level security;

-- RLS Policies: Authenticated users can manage their own incidents
create policy "Users can manage their own incidents"
    on public.incidents
    for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "Users can manage points for their own incidents"
    on public.incident_points
    for all
    using (
        exists (
            select 1 from public.incidents
            where incidents.id = incident_points.incident_id
            and incidents.user_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1 from public.incidents
            where incidents.id = incident_points.incident_id
            and incidents.user_id = auth.uid()
        )
    );

create policy "Users can manage synchronized p2p messages"
    on public.p2p_messages
    for all
    using (auth.uid() = synced_by_user_id)
    with check (auth.uid() = synced_by_user_id);
