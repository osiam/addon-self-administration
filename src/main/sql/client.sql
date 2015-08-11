--
-- Example self-administration client, please sync with the attribute values in
-- the addon-self-administration.properties. Has to be imported in the database
-- of the auth-server, before you deploy the addon-self-administration!
--

INSERT INTO osiam_client (internal_id, access_token_validity_seconds, client_secret, id,
                          implicit_approval, redirect_uri, refresh_token_validity_seconds, validity_in_seconds)
VALUES (10, 300, 'super-secret', 'addon-self-administration-client',
        FALSE, 'http://localhost:8080/addon-self-administration', 0, 0);

INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'ADMIN');
INSERT INTO osiam_client_grants (id, grants) VALUES (10, 'client_credentials');
