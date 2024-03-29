--
-- users
--
insert into users (name, password, email_address, auth_claim, pw_reset_claim, pw_reset_auth_claim, verification_claim, is_verified, auth_provider, auth_provider_id, auth_provider_profile_img_url, auth_provider_username, application_id)
values ('me', 'password', 'me@localhost', 'auth', 'pw_reset', 'pw_reset_auth', 'verification', true, 'GOOGLE', '114746878003745038229', null, null, 'FEEDGEARS_RSS');
--
-- queue_definitions
--
insert into queue_definitions(queue_ident, queue_title, queue_desc, queue_feed_generator, transport_ident, username, queue_status, copyright, language, queue_img_src, queue_img_transport_ident, category_term, category_label, category_scheme, category_value, category_domain, last_deployed_timestamp, is_authenticated)
values ('cnn_inbound', 'cnn_inbound', 'cnn_inbound', 'cnn_inbound-generator', '5', 'me', 'ENABLED', 'Copyright (c) 2023 Lost Sidewalk Software, Inc. All Rights Reserved.', 'en-US', null, null, null, null, null, null, null, null, false);
--
-- subscription_definitions
--
insert into subscription_definitions(queue_id, username, title, url, query_type, import_schedule)
values(
    (select id from queue_definitions where queue_ident = 'cnn_inbound'),
    'me',
    'CNN-INBOUND CNN Top Stories',
    'http://rss.cnn.com/rss/cnn_topstories.rss',
    'RSS',
    'A'
);
--
-- rule_set_definitions
--
insert into rule_set_definitions(username, rule_set_name, rules) values (
    'me',
    'cnn_inbound_rules',
    '[]'
);

insert into rule_set_definitions(username, rule_set_name, rules) values (
    'me',
    'cnn_top_stories_rules',
    '[]'
);
--
-- queue_import_rule_sets
--
insert into queue_import_rule_sets (queue_id, rule_set_id) values (
    (select id from queue_definitions where queue_ident = 'cnn_inbound'),
    (select id from rule_set_definitions where rule_set_name = 'cnn_inbound_rules')
);
--
-- subscription_import_rule_sets
--
insert into subscription_import_rule_sets (subscription_id, rule_set_id) values (
    (select id from subscription_definitions where title = 'CNN-INBOUND CNN Top Stories'),
    (select id from rule_set_definitions where rule_set_name = 'cnn_top_stories_rules')
);
--
-- roles
--
insert into roles (name, application_id)
values ('admin', 'FEEDGEARS_RSS');
--
-- features_in_roles
--
-- insert into features_in_roles (role, feature_cd)
-- values ('admin', 'login');
--
-- framework_config
--
insert into framework_config(user_id,settings_group,attr_name,attr_value)
values ((select id from users where name = 'me'), 'notifications', 'disabled', 'true');
