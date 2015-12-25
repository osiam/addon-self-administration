--
-- Example self-administration client, please sync with the attribute values in
-- the addon-self-administration.properties. Has to be imported in the database
-- of OSIAM, before you deploy the addon-self-administration!
--

INSERT INTO osiam_client VALUES (
  10, -- internal_id
  300, -- access_token_validity_seconds
  'super-secret', -- client_secret
  'addon-self-administration-client', -- id
  FALSE, -- implicit_approval
  'http://localhost:8080/addon-self-administration', -- redirect_uri
  0, -- refresh_token_validity_seconds
  0 -- validity_in_seconds
);

INSERT INTO osiam_client_scopes VALUES (10, 'ADMIN');
INSERT INTO osiam_client_grants VALUES (10, 'client_credentials');
