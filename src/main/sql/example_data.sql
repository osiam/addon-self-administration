--
-- DEPRECATED: replaced by client.sql
--

INSERT INTO osiam_client (internal_id, accesstokenvalidityseconds, client_secret, id,
                          implicit_approval, redirect_uri, refreshtokenvalidityseconds, validityinseconds)
VALUES (10, 300, 'super-secret', 'addon-self-administration-client',
        FALSE, 'http://localhost:8080/addon-self-administration', 0, 0);
INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'GET');
INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'POST');
INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'PUT');
INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'PATCH');
INSERT INTO osiam_client_scopes (id, scope) VALUES (10, 'DELETE');
INSERT INTO osiam_client_grants (id, grants) VALUES (10, 'client_credentials');
