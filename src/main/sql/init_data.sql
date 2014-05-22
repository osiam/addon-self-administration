--
-- Extension data, according to the attribute values in the addon-self-administration.properties
-- Have to be imported in the database of the resource server, before you deploy the addon-self-administration!
--

INSERT INTO scim_extension VALUES (5, 'urn:org.osiam:scim:extensions:addon-self-administration');

INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension_internal_id)
	VALUES (6, false, 'activationToken', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension_internal_id)
	VALUES (7, false, 'oneTimePassword', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension_internal_id)
	VALUES (8, false, 'emailConfirmToken', 'STRING', 5);
INSERT INTO scim_extension_field (internal_id, is_required, name, type, extension_internal_id)
	VALUES (9, false, 'tempMail', 'STRING', 5);