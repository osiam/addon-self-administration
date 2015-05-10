For the integration of "password change" and "primary email change" in your app you need to define a client side mechanism for calling the self-administration's HTTP API and to receive the user requests. The user agent will not directly speak with the self-administration.

You need also to enhance the request with the authorization header and a valid access token before sending to the registration modules HTTP endpoint.

The base URI for the module is: **http://HOST:PORT/osiam-addon-self-administration**