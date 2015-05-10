The change password mechanism has also multiple steps.

First of all the user has to indicate on client side that he lost his password. Then the client will send a request to the registration module. Osiam will now generate a one time password and will send the user an email with a link in the content, including the one time password. This url will also be hosted on client side and will point to the 'osiam.mail.passwordlost.linkprefix' config property.
The user need to go to the url from the email's content and has to enter his new password.
Then the request must be submitted to the registration module where the one time password verification will be triggered and if this was successful the new password will be saved and the user is able to login with the new password.

There are three HTTP endpoint:

<table>
    <tr>
        <th> URI </th>
        <th> HTTP-Method </th>
        <th> Access </th>
        <th> Params </th>
        <th> Email Template File Names </th>
        <th> Description </th>
    </tr>
    <tr>
        <td> /password/lostForm </td>
        <td> GET </td>
        <td> no access token needed </td>
        <td> 'oneTimePassword', 'userId' </td>
        <td> - </td>
        <td> Will provide a HTML form with the required fields for change password including validation.
         The request will be submitted to the URL configured in the 'osiam.html.password.url' parameter. </td>
    </tr>
    <tr>
        <td> /password/lost/{userId} </td>
        <td> POST </td>
        <td> access token in the Authorization header as HTTP Bearer authorization</td>
        <td> - </td>
        <td> lostpassword-*.html, lostpassword.html, lostpassword-default-*.html, lostpassword-default.html </td>
        <td> This will generate a one time password an sending the user an email with a confirmation link
         pointing to the 'osiam.mail.passwordlost.linkprefix' config parameter including his one time password.
         The response will be the HTTP status code. </td>
    </tr>
    <tr>
        <td> /password/change </td>
        <td> POST </td>
        <td> access token in the Authorization header as HTTP Bearer authorization</td>
        <td> 'oneTimePassword', 'newPassword' </td>
        <td> - </td>
        <td> This will validate the one time password and save the new password if the validation will be successful.
          The response will be the HTTP status code and the previously updated user if successful. </td>
    </tr>
</table>