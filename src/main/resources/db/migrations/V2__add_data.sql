--
-- users
--
insert into users (name, password, email_address, auth_claim, pw_reset_claim, pw_reset_auth_claim, verification_claim, is_verified, auth_provider, auth_provider_id, auth_provider_profile_img_url, auth_provider_username, application_id)
values ('me', 'password', 'me@localhost', 'auth', 'pw_reset', 'pw_reset_auth', 'verification', true, 'GOOGLE', '114746878003745038229', null, null, 'FEEDGEARS_RSS');
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
