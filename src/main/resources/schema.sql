
drop table if exists staging_posts cascade;

create table staging_posts (
    id bigserial not null primary key,
    post_title varchar(512) not null,
    post_desc varchar(1024) not null,
    post_url varchar(512) not null,
    post_img_url varchar(512),
    importer_id varchar(256) not null,
    tag_name varchar(256) not null,
    object_source json not null,
    import_timestamp timestamp with time zone,
    post_status varchar(64),
    is_published boolean not null default false,
    post_hash varchar(64) not null
);
