--
-- users table
--
drop table if exists users cascade;

create table users
(
  id serial,
  name varchar(100) unique not null,
  password varchar(256) not null,
  email_address varchar(512) unique not null,
  auth_claim varchar(256),
  pw_reset_claim varchar(256),
  pw_reset_auth_claim varchar(256),
  verification_claim varchar(256),
  is_verified boolean not null default false,
  subscription_status varchar(128),
  subscription_exp_date timestamp with time zone,
  customer_id varchar(256),
  auth_provider varchar(64) not null,
  auth_provider_id varchar(256),
  auth_provider_profile_img_url varchar(1024),
  auth_provider_username varchar(256),

  primary key (id)
);
--
-- feed_definitions table
--
drop table if exists feed_definitions cascade;

create table feed_definitions (
    id bigserial not null,
    feed_ident varchar(256) not null,
    feed_title varchar(512) not null,
    feed_desc varchar(1024),
    feed_generator varchar(512),
    transport_ident varchar(256) unique not null,
    username varchar(100) not null references users(name) on delete cascade,
    feed_status varchar(64) not null,
    export_config json,
    copyright varchar(1024),
    language varchar(16) not null,
    feed_img_src varchar(10240),
    feed_img_transport_ident varchar(256),
    category_term varchar(256),
    category_label varchar(256),
    category_scheme varchar(256),
    category_value varchar(256),
    category_domain varchar(256),
    last_deployed_timestamp timestamp with time zone,
    is_deleted boolean not null default false,
    is_authenticated boolean not null default false,

    primary key (id)
);
--
-- feed_credentials table
--
drop table if exists feed_credentials cascade;

create table feed_credentials (
    id bigserial not null,
    transport_ident varchar(256) not null references feed_definitions(transport_ident) on delete cascade,
    username varchar(100) not null,
    password varchar(256) not null,

    primary key (id)
);
--
-- staging_posts table
--
drop table if exists staging_posts cascade;

-- TODO: rename post_img_url -> post_image_url
create table staging_posts (
    id bigserial not null,
    post_title json not null,
    post_desc json not null,
    post_contents json,
    post_media json,
    post_itunes json,
    post_url varchar(1024) not null,
    post_urls json,
    post_img_url varchar(1024),
    post_img_transport_ident varchar(256),
    importer_id varchar(256) not null,
    importer_desc varchar(512),
    query_id bigserial not null,
    feed_id bigserial not null references feed_definitions(id) on delete cascade,
    import_timestamp timestamp with time zone,
    is_published boolean not null default false,
    post_read_status varchar(64),
    post_pub_status varchar(64),
    post_hash varchar(64) not null,
    username varchar(100) not null references users(name) on delete cascade,
    post_comment varchar(2048),
    post_rights varchar(1024),
    contributors json,
    authors json,
    post_categories json,
    publish_timestamp timestamp with time zone,
    expiration_timestamp timestamp with time zone,
    enclosures json,
    last_updated_timestamp timestamp with time zone,

    primary key(id)
);
--
-- query_definitions table
--
drop table if exists query_definitions cascade;

create table query_definitions (
    id bigserial not null,
    feed_id bigserial not null references feed_definitions(id) on delete cascade,
    username varchar(100) not null references users(name) on delete cascade,
    query_title varchar(512),
    query_image_url varchar(1024),
    query_text varchar(2048) not null,
    query_type varchar(64) not null,
    query_config json,

    primary key(id)
);
--
-- query_metrics table
--
drop table if exists query_metrics cascade;

create table query_metrics (
    id bigserial not null,
    query_id bigserial not null,
    http_status_code integer,
    http_status_message varchar(512),
    redirect_feed_url varchar(1024),
    redirect_http_status_code integer,
    redirect_http_status_message varchar(512),
    import_timestamp timestamp with time zone,
    import_ct integer,
    persist_ct integer,
    skip_ct integer,
    archive_ct integer,
    error_type varchar(64),
    error_detail varchar(1024),

    primary key(id)
);
--
-- feed_discovery_info table
--
drop table if exists feed_discovery_info cascade;

create table feed_discovery_info (
    id bigserial not null,
    feed_url varchar(1024) not null,
    http_status_code integer,
    http_status_message varchar(512),
    redirect_feed_url varchar(1024),
    redirect_http_status_code integer,
    redirect_http_status_message varchar(512),
    title json,
    description json,
    feed_type varchar(64),
    author varchar(256),
    copyright varchar(1024),
    docs varchar(1024),
    encoding varchar(64),
    generator varchar(512),
    image json,
    icon json,
    language varchar(16),
    link varchar(1024),
    managing_editor varchar(256),
    published_date timestamp with time zone,
    supported_types json,
    web_master varchar(256),
    uri varchar(1024),
    categories json,
    sample_entries json,
    is_url_upgradeable boolean not null default false,
    error_type varchar(64),
    error_detail varchar(1024),

    primary key(id)
);
--
--
--
drop table if exists thumbnails cascade;

create table thumbnails (
    id bigserial not null,
    img_src varchar(10240),

    primary key(id)
);
--
-- roles table
--
drop table if exists roles cascade;

create table roles
(
  id serial,
  name varchar(256) unique not null,

  primary key(name)
);
--
-- features_in_roles table
--
drop table if exists features_in_roles cascade;

create table features_in_roles
(
  id serial,
  feature_cd varchar(100) not null,
  role varchar(100) not null references roles(name) on delete cascade,

  primary key(id)
);
--
-- users_in_roles table
--
drop table if exists users_in_roles cascade;

create table users_in_roles
(
  id serial,
  username varchar(100) not null references users(name) on delete cascade,
  role varchar(100) not null references roles(name) on delete cascade,

  primary key (id)
);
--
-- framework_config table
--
drop table if exists framework_config cascade;

create table framework_config
(
  id serial not null,
  user_id integer not null references users(id) on delete cascade,
  settings_group varchar(256) not null,
  attr_name varchar(256) not null,
  attr_value varchar(4000) not null,
  unique(user_id, settings_group, attr_name),

  primary key (id)
);
--
-- indexes
--
drop index if exists idx_staging_posts_post_pub_status;
drop index if exists idx_staging_posts_post_hash;
drop index if exists idx_staging_posts_feed_id;
drop index if exists idx_staging_posts_username;
drop index if exists idx_feed_definitions_username;
drop index if exists idx_feed_definitions_transport_ident;
drop index if exists idx_feed_credentials_transport_ident;
drop index if exists idx_query_definitions_feed_id;
drop index if exists idx_query_definitions_username;
drop index if exists idx_query_metrics_query_id;
drop index if exists idx_roles_name;
drop index if exists idx_features_in_roles_role;
drop index if exists idx_users_email_address;
drop index if exists idx_users_customer_id;
drop index if exists idx_users_auth_provider;
drop index if exists idx_users_name;

create index idx_staging_posts_post_pub_status on staging_posts(post_pub_status);
create index idx_staging_posts_post_hash on staging_posts(post_hash);
create index idx_staging_posts_feed_id on staging_posts(feed_id);
create index idx_staging_posts_username on staging_posts(username);
create index idx_feed_definitions_username on feed_definitions(username);
create index idx_feed_definitions_transport_ident on feed_definitions(transport_ident);
create index idx_feed_credentials_transport_ident on feed_credentials(transport_ident);
create index idx_query_definitions_feed_id on query_definitions(feed_id);
create index idx_query_definitions_username on query_definitions(username);
create index idx_query_metrics_query_id on query_metrics(query_id);
create index idx_roles_name on roles(name);
create index idx_features_in_roles_role on features_in_roles(role);
create index idx_users_email_address on users(email_address);
create index idx_users_customer_id on users(customer_id);
create index idx_users_auth_provider on users(auth_provider);
create index idx_users_name on users(name);
