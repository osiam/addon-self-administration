- [User registration](#user-registration)
- [HTTP endpoints](#http-endpoints)
- [Configuration](#configuration)
 - [Configuration values](#configuration-values)
    - [org.osiam.html.form.usernameEqualsEmail](#orgosiamhtmlformusernameequalsemail)
    - [org.osiam.html.form.password.length](#orgosiamhtmlformpasswordlength)
    - [org.osiam.html.form.fields](#orgosiamhtmlformfields)
    - [org.osiam.html.form.extensions](#orgosiamhtmlformextensions)
- [Default Files](#default-files)

# User registration
The registration will be done via a double opt in mechanism.

In the first step the user will register at osiam via the addon-self-administration application.
Then an email will be send to the address provided in the first step. The content of that email will
include a registration link with an activation token.
The second step for the user will be to confirm his email address by going to that url.
After this the user will be activated by the addon-self-administration. This will be done by set the active flag of the User to true.

## HTTP endpoints

<table>
    <tr>
        <th> URI </th>
        <th> HTTP-Method </th>
        <th> Params </th>
        <th> Email Template File Names </th>
        <th> Shown page on success </th>
        <th> Description </th>
    </tr>
    <tr>
        <td> /registration </td>
        <td> GET </td>
        <td> - </td>
         <td> - </td>
         <td> registration.html </td>
        <td> Will provide a HTML form with the required fields for registration including validation. </td>
    </tr>
    <tr>
        <td> /registration </td>
        <td> POST </td>
        <td> - </td>
        <td> registration-*.html, registration.html, registration-default-*.html, registration-default.html </td>
         <td> registrationSuccess.html </td>
        <td> Will create the user with activation token. The user will be disabled until email confirmation  
        and he is not able to login yet. Also a email will be send to the user with the link pointing back to 
        the selfadministration</td>
    </tr>
    <tr>
        <td> /registration/activation </td>
        <td> GET </td>
        <td> 'userId', 'activationToken' from the email's confirmation link. </td>
        <td> - </td>
         <td> activationSuccess.html </td>
        <td> The activation token will be validated and the user will be enabled for login if the validation 
         was successful.
 </td>
    </tr>
</table>

## Configuration

For the user registration you need to configure some variables in the file 

`/etc/osiam/osiam-self-administration.properties`

The main configuration of this file is described [here](configuration.md#configuring-osiam)

### Configuration values

####org.osiam.html.form.usernameEqualsEmail

Configures if the email address of the user is taken as username or if he can type in a separate user name.
The value can be true of false. The default value is true.

####org.osiam.html.form.password.length

Describes the min length of the password of the user. The default is 8.

####org.osiam.html.form.fields

To be sure that only fields of an User will be saved in the database the wanted fields can be configured here.

There are some standard fields that don't need (and can't) be configured.

The email and the password are always part of the selfadministration. If the value org.osiam.html.form.usernameEqualsEmail is set to false the username is also part of the selfadministration.
 
If you are using the standard registration.html template these fields also configure which fields will be shown in the web browser.

A list of all possible fields are:

```
org.osiam.html.form.fields=formattedName\
,familyName\
,givenName\
,middleName\
,givenName\
,honorificPrefix\
,honorificSuffix\
,displayName\
,nickName\
,profileUrl\
,title\
,preferredLanguage\
,locale\
,timezone\
,confirmPassword\
,email\
,phoneNumber\
,im\
,photo\
,formattedAddress\
,streetAddress\
,locality\
,region\
,postalCode\
,country
```

So for example if your configuration looks like this:

org.osiam.html.form.fields=nickName

The String fields email, password and nickName will be shown. The field userName will not be shown becauce the default value of org.osiam.html.form.usernameEqualsEmail is true and so the email will be taken as userName.

Since you can configure the shown fields in the webbrowser with this property you don't have to delete the fields in the index.html page.

If you don't have 'confirmPassword' as part of the configuration the user only has to type in his passwort once to register.

####org.osiam.html.form.extensions

I you wan't to show own extension fields in the self administration you have to do 3 things (besides from [registering](https://github.com/osiam/server/wiki/detailed_reference_installation-1.0#configuring-scim-extension) them in the database)

First you have to add them to the register.html file

To make this as easy as possible we have added a profile in the htmlfield.html file.
Like this you can add the extension field in a similar way like a standard field.

```
 <div th:replace="htmlfield :: extensioninput('<urn>', '<field>', '<type>', <required>)"></div>
```

Second to have to register the extension in the osiam-self-administration.properties file by adding them to the field org.osiam.html.form.extensions like the following

 extensions['<urn>'].fields['<field>']

third you have to add the placeholder in the language files. like the following
registration.<urn>.<field>=<placeholder>

Example:

We have the extension: urn:client:extension

This extension has the fields age and gender

We wan't to add these fields under the country field in the registration.html file.

```
<div th:replace="htmlfield :: input('country', 'text', false)"></div>
<div th:replace="htmlfield :: extensioninput('urn:client:extension', 'age', 'text', false)"></div>
<div th:replace="htmlfield :: extensioninput('urn:client:extension', 'gender', 'text', false)"></div>
```

No we register them in the osiam-self-administration.properties file

```
org.osiam.html.form.extensions=extensions['urn:client:extension'].fields['age']\
,extensions['urn:client:extension'].fields['gender']
```

At last we put the placeholder in the locale/registration.properties file

```
registration.urn\:client\:extension.age=age
registration.urn\:client\:extension.gender=gender
```

Attention! Like you see we have an : in our extension. To be able to still put it into the local file we have to escape these.

# Default Files

With the addon-self-administration also the folder addon-self-administration will be delivered. Please put this folder at the place where you have your osiam-self-administration.properties file.

The folder contains the following subfolders and files.

**addon-self-administration/locale/registration.properties**

This files contains all placeholders for the html pages. If you wan't to translate the selfadministration to your own language please copy this file and rename it to your locale. An german example (registration_de_DE.properties) is also placed in this folder.

**addon-self-administration/resources/css/bootstrap.min.css**

default css file from bootstrap. You don't have to change it.

**addon-self-administration/resources/css/style.css**

Holds the styles of different html elements from the html pages.

**addon-self-administration/templates/web/registration.html**

This is the main html page. All standard user fields are already part of this file an can be switched on and of by configuration of the parameters [org.osiam.html.form.fields](#orgosiamhtmlformfields).

Also your own extension fields can easily be added descripted at [org.osiam.html.form.extensions](#orgosiamhtmlformextensions)

**addon-self-administration/templates/web/registrationSuccess.html**

This page will be called after the User has been successfully registered himself and after a registration mail was sent to the user.

**addon-self-administration/templates/web/activationSuccess.html**

This page will be called after the User clicked successfully on the activation link send to him by mail after registration 

**addon-self-administration/templates/web/self_administration_error.html**

This page will be called if an internal error happens. If your Log Level is INFO or lower also the exact error message will be written here. This will probably help you to be able to find any problems in an easier way. 

**addon-self-administration/templates/web/htmlfields.html**

In normal cases you don't have to change this html page. It is used from the registration.html to create all default and extension input fields dynamically.
