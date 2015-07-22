--
-- Extension that stores the one time tokens for all mechanisms. Has to be
-- imported in the database of the resource server, before you deploy
-- the addon-self-administration!
--

INSERT INTO scim_extension VALUES (5, 'urn:org.osiam:scim:extensions:addon-self-administration');

INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension)
VALUES (6, FALSE, 'activationToken', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension)
VALUES (7, FALSE, 'oneTimePassword', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension)
VALUES (8, FALSE, 'emailConfirmToken', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension)
VALUES (9, FALSE, 'tempMail', 'STRING', 5);
