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
    transport_ident varchar(256) not null,
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

    primary key (id)
);
--
-- staging_posts table
--
drop table if exists staging_posts cascade;

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
    feed_id bigserial not null references feed_definitions(id) on delete cascade,
    object_source json not null,
    source_name varchar(256),
    source_url varchar(1024),
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
    query_id bigserial not null references query_definitions(id) on delete cascade,
    http_status_code integer,
    http_status_message varchar(512),
    redirect_feed_url varchar(1024),
    redirect_http_status_code integer,
    redirect_http_status_message varchar(512),
    import_timestamp timestamp with time zone,
    import_ct integer,
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
