--
-- users
--
insert into users (name, password, email_address, auth_claim, pw_reset_claim, pw_reset_auth_claim, verification_claim, is_verified, auth_provider, auth_provider_id, auth_provider_profile_img_url, auth_provider_username)
values ('me', 'password', 'michaeledwardharris@gmail.com', 'auth', 'pw_reset', 'pw_reset_auth', 'verification', true, 'GOOGLE', '114746878003745038229', null, null);
--
-- feed_definitions
--
insert into feed_definitions(feed_ident, feed_title, feed_desc, feed_generator, transport_ident, username, is_active, export_config, copyright, language, feed_img_src, feed_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp)
values ('programming', 'programming', 'programming', 'programming-generator', '1', 'me', true, '{ }', 'Copyright (c) 2022 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null);

insert into feed_definitions(feed_ident, feed_title, feed_desc, feed_generator, transport_ident, username, is_active, copyright, language, feed_img_src, feed_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp)
values ('elder_scrolls', 'elder_scrolls', 'elder_scrolls', 'elder_scrolls-generator', '2', 'me', true, 'Copyright (c) 2022 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null);

insert into feed_definitions(feed_ident, feed_title, feed_desc, feed_generator, transport_ident, username, is_active, copyright, language, feed_img_src, feed_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp)
values ('linux', 'linux', 'linux', 'linux-generator', '3', 'me', true, 'Copyright (c) 2022 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null);

insert into feed_definitions(feed_ident, feed_title, feed_desc, feed_generator, transport_ident, username, is_active, copyright, language, feed_img_src, feed_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp)
values ('sci_tech', 'sci_tech', 'sci_tech', 'sci_tech-generator', '4', 'me', true, 'Copyright (c) 2022 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null);

insert into feed_definitions(feed_ident, feed_title, feed_desc, feed_generator, transport_ident, username, is_active, copyright, language, feed_img_src, feed_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp)
values ('cnn_inbound', 'cnn_inbound', 'cnn_inbound', 'cnn_inbound-generator', '5', 'me', true, 'Copyright (c) 2022 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null);
--
-- roles
--
insert into roles (name)
values ('admin');
--
-- features_in_roles
--
-- insert into features_in_roles (role, feature_cd)
-- values ('admin', 'login');
--
-- query_definitions
--
insert into query_definitions(feed_ident, username, query_text, query_type, query_config)
values ('programming', 'me', '(java OR jdk) NOT (indonesia)', 'NEWSAPIV2_HEADLINES', '{ "sources" : ["ABC_NEWS", "AL_JAZEERA_ENGLISH"] }');

insert into query_definitions(feed_ident, username, query_text, query_type, query_config)
values ('elder_scrolls', 'me', '(skyrim OR elder scrolls)', 'NEWSAPIV2_HEADLINES', '{ "sources" : [] }');

insert into query_definitions(feed_ident, username, query_text, query_type)
values ('linux', 'me', '(linux OR ubuntu OR centos OR slackware OR debian)', 'NEWSAPIV2_HEADLINES');

insert into query_definitions(feed_ident, username, query_text, query_type)
values ('sci_tech', 'me', '(science OR technology)', 'NEWSAPIV2_HEADLINES');

insert into query_definitions(feed_ident, username, query_text, query_type)
values('cnn_inbound', 'me', 'http://rss.cnn.com/rss/cnn_topstories.rss', 'RSS');

insert into query_definitions(feed_ident, username, query_text, query_type)
values('cnn_inbound', 'me', 'http://rss.cnn.com/rss/cnn_world.rss', 'RSS');
--
-- framework_config
--
insert into framework_config(user_id,settings_group,attr_name,attr_value)
values ((select id from users where name = 'me'), 'notifications', 'disabled', 'true');
