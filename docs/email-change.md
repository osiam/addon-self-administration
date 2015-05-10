The change e-mail mechanism has also multiple steps.

First of all the user has to indicate on client side that he want to change his e-mail address.
Then the client will send a request including the new e-mail to the registration module.
Osiam will now generate a confirmation token and will send the user an email with a link in the content,
including the confirmation token. This url will also be hosted on client side and will point
to the 'osiam.mail.emailchange.linkprefix' config property.

The user need confirm his e-mail address by going to the url from the email's content.
Then the request must be submitted to the registration module where the confirmation token verification will be triggered and if this was successful the new e-mail address will be stored. Finally an information e-mail will be send to the user's old e-mail address.

There are three HTTP endpoints:

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
     <td> /email </td>
     <td> GET </td>
     <td> no access token needed </td>
     <td> - </td>
     <td> - </td>
     <td> Will provide a HTML form with the required fields for change e-mail including validation.
      The request will be submitted to the URL configured in the 'osiam.web.email.url' parameter. </td>
 </tr>
 <tr>
     <td> /email/change </td>
     <td> POST </td>
     <td> access token in the Authorization header as HTTP Bearer authorization</td>
     <td> 'newEmailValue' the new user's e-mail address. </td>
     <td> changeemail-*.html, changeemail.html, changeemail-default-*.html, changeemail-default.html </td>
     <td> This will generate a confirmation token an sending the user an email with a confirmation link
      pointing to the 'osiam.web.emailchange.linkprefix' config parameter including the confirmation token.
      The response will be the HTTP status code. </td>
 </tr>
 <tr>
     <td> /email/confirm </td>
     <td> POST </td>
     <td> access token in the Authorization header as HTTP Bearer authorization</td>
     <td> 'userId', 'confirmToken' </td>
     <td> changeemailinfo-*.html, changeemailinfo.html, changeemailinfo-default-*.html, changeemailinfo-default.html </td>
     <td> This will validate the confirmToken and save the new e-mail if the validation will be successful.
       The response will be the HTTP status code and the previously updated user if successful. </td>
 </tr>
</table>