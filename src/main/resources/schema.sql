--
-- users table
--
drop table if exists users cascade;

create table users
(
  id bigserial,
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
  application_id varchar(32) not null,

  primary key (id)
);
--
-- queue_definitions table
--
drop table if exists queue_definitions cascade;

create table queue_definitions (
    id bigserial,
    queue_ident varchar(256) not null,
    queue_title varchar(512),
    queue_desc varchar(1024),
    queue_feed_generator varchar(512),
    transport_ident varchar(256) unique not null,
    username varchar(100) not null references users(name) on delete cascade,
    queue_status varchar(64) not null,
    export_config json,
    copyright varchar(1024),
    language varchar(16) not null,
    queue_img_src varchar(10240),
    queue_img_transport_ident varchar(256),
    category_term varchar(256),
    category_label varchar(256),
    category_scheme varchar(256),
    category_value varchar(256),
    category_domain varchar(256),
    last_deployed_timestamp timestamp with time zone,
    is_deleted boolean not null default false,
    is_authenticated boolean not null default false,
    created timestamp with time zone not null default current_timestamp,
    last_modified timestamp with time zone,

    unique(username, queue_ident),

    primary key (id)
);
--
-- queue_credentials table
--
drop table if exists queue_credentials cascade;

create table queue_credentials (
    id serial,
    queue_id integer references queue_definitions(id) on delete cascade,
    username varchar(100) not null references users(name) on delete cascade,
    basic_username varchar(100) not null,
    basic_password varchar(256) not null,
    created timestamp with time zone not null default current_timestamp,
    last_modified timestamp with time zone,
    unique(queue_id, basic_username),

    primary key (id)
);

-- set the Id starting value to 1048576
alter sequence queue_credentials_id_seq restart with 1048576;

--
-- staging_posts table
--
drop table if exists staging_posts cascade;

-- TODO: rename post_img_url -> post_image_url
create table staging_posts (
    id serial,
    post_title json not null,
    post_desc json not null,
    post_contents json,
    post_media json,
    post_itunes json,
    post_url varchar(1024),
    post_urls json,
    post_img_url varchar(1024),
    post_img_transport_ident varchar(256),
    importer_id varchar(256) not null,
    importer_desc varchar(512),
    subscription_id integer,
    queue_id integer references queue_definitions(id) on delete cascade,
    import_timestamp timestamp with time zone,
    is_published boolean not null default false,
    post_read_status varchar(64),
    post_pub_status varchar(64),
    post_hash varchar(64),
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
    created timestamp with time zone not null default current_timestamp,
    last_modified timestamp with time zone,

    unique(queue_id, post_hash),

    primary key(id)
);

-- set the Id starting value to 1048576
alter sequence staging_posts_id_seq restart with 1048576;

--
-- subscription_definitions table
--
drop table if exists subscription_definitions cascade;

create table subscription_definitions (
    id bigserial,
    queue_id integer references queue_definitions(id) on delete cascade,
    username varchar(100) not null references users(name) on delete cascade,
    title varchar(512),
    img_url varchar(1024),
    url varchar(2048) not null,
    query_type varchar(64) not null,
    import_schedule varchar(32),
    query_config json,
    unique(queue_id, url),

    primary key(id)
);
--
-- subscription_metrics table
--
drop table if exists subscription_metrics cascade;

create table subscription_metrics (
    id bigserial,
    subscription_id integer not null references subscription_definitions(id) on delete cascade,
    http_status_code integer,
    http_status_message varchar(512),
    redirect_feed_url varchar(1024),
    redirect_http_status_code integer,
    redirect_http_status_message varchar(512),
    import_timestamp timestamp with time zone,
    import_schedule varchar(32),
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
    id bigserial,
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
    id bigserial,
    img_src varchar(10240),

    primary key(id)
);
--
-- roles table
--
drop table if exists roles cascade;

create table roles
(
  id bigserial,
  name varchar(256) unique not null,
  application_id varchar(32) not null,

  primary key(name)
);
--
-- features_in_roles table
--
drop table if exists features_in_roles cascade;

create table features_in_roles
(
  id bigserial,
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
  id bigserial,
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
  id bigserial,
  user_id integer not null references users(id) on delete cascade,
  settings_group varchar(256) not null,
  attr_name varchar(256) not null,
  attr_value varchar(4000) not null,
  unique(user_id, settings_group, attr_name),

  primary key (id)
);
--
--
--
drop table if exists theme_config cascade;

create table theme_config
(
  id bigserial,
  user_id integer not null references users(id) on delete cascade,
  light_theme json,
  dark_theme json,

  primary key (id)
);
--
-- api_keys
--
drop table if exists api_keys cascade;

create table api_keys
(
  id bigserial,
  user_id integer unique not null references users(id) on delete cascade,
  api_key varchar(512) not null,
  api_secret varchar(512) not null,
  application_id varchar(32) not null,

  primary key (id)
);

--
-- indexes
--
drop index if exists idx_staging_posts_post_pub_status;
drop index if exists idx_staging_posts_post_hash;
drop index if exists idx_staging_posts_queue_id;
drop index if exists idx_staging_posts_username;
drop index if exists idx_queue_definitions_username;
drop index if exists idx_queue_definitions_transport_ident;
drop index if exists idx_queue_credentials_queue_id;
drop index if exists idx_subscription_definitions_queue_id;
drop index if exists idx_subscription_definitions_username;
drop index if exists idx_subscription_metrics_subscription_id;
drop index if exists idx_roles_name;
drop index if exists idx_features_in_roles_role;
drop index if exists idx_users_email_address;
drop index if exists idx_users_customer_id;
drop index if exists idx_users_auth_provider;
drop index if exists idx_users_name;
drop index if exists idx_framework_config_user_id;
drop index if exists idx_theme_config_user_id;
drop index if exists idx_api_keys_user_id;
drop index if exists idx_api_keys_api_key;

create index idx_staging_posts_post_pub_status on staging_posts(post_pub_status);
create index idx_staging_posts_post_hash on staging_posts(post_hash);
create index idx_staging_posts_queue_id on staging_posts(queue_id);
create index idx_staging_posts_username on staging_posts(username);
create index idx_queue_definitions_username on queue_definitions(username);
create index idx_queue_definitions_transport_ident on queue_definitions(transport_ident);
create index idx_queue_credentials_queue_id on queue_credentials(queue_id);
create index idx_subscription_definitions_queue_id on subscription_definitions(queue_id);
create index idx_subscription_definitions_username on subscription_definitions(username);
create index idx_subscription_metrics_subscription_id on subscription_metrics(subscription_id);
create index idx_roles_name on roles(name);
create index idx_features_in_roles_role on features_in_roles(role);
create index idx_users_email_address on users(email_address);
create index idx_users_customer_id on users(customer_id);
create index idx_users_auth_provider on users(auth_provider);
create index idx_users_name on users(name);
create index idx_framework_config_user_id on framework_config(user_id);
create index idx_theme_config_user_id on theme_config(user_id);
create index idx_api_keys_user_id on api_keys(user_id);
create index idx_api_keys_api_key on api_keys(api_key);
--
-- end
--
