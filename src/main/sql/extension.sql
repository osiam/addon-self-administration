--
-- Extension that stores the one time tokens for all mechanisms. Has to be imported in the
-- database of OSIAM, before you deploy the addon-self-administration!
--

INSERT INTO scim_extension VALUES (
  5, -- internal_id
  'urn:org.osiam:scim:extensions:addon-self-administration' -- urn
);

INSERT INTO scim_extension_field VALUES (
  6, -- internal_id
  'activationToken', -- name
  FALSE, -- required
  'STRING', -- type
  5 -- extension
);
INSERT INTO scim_extension_field VALUES (
  7, -- internal_id
  'oneTimePassword', -- name
  FALSE, -- required
  'STRING', -- type
  5 -- extension
);
INSERT INTO scim_extension_field VALUES (
  8, -- internal_id
  'emailConfirmToken', -- name
  FALSE, -- required
  'STRING', -- type
  5 -- extension
);
INSERT INTO scim_extension_field VALUES (
  9, -- internal_id
  'tempMail', -- name
  FALSE, -- required
  'STRING', -- type
  5 -- extension
);
