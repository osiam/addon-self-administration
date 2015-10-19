- [User registration](#user-registration)
- [HTTP endpoints](#http-endpoints)
- [Configuration](#configuration)
 - [Configuration values](#configuration-values)
    - [org.osiam.html.form.usernameEqualsEmail](#orgosiamhtmlformusernameequalsemail)
    - [org.osiam.html.form.password.length](#orgosiamhtmlformpasswordlength)
    - [org.osiam.html.form.fields](#orgosiamhtmlformfields)
    - [org.osiam.html.form.extensions](#orgosiamhtmlformextensions)
- [Default Files](#default-files)
- [Callback plugin](#callback-plugin)

# User registration

The registration will be done via a double opt in mechanism:

In the first step the user will register at osiam via the
addon-self-administration application. Then an email will be send to the address
provided in the first step. The content of that email will include a
registration link with an activation token.

The second step for the user will be to confirm his email address by going to
that url. After this the user will be activated by the
addon-self-administration. This will be done by set the active flag of the User
to true.

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

To be sure that only fields of a User will be saved in the database the wanted
fields can be configured here.

There are some standard fields that don't need (and can't) be configured.

The email and the password are always part of the self administration. If the
value `org.osiam.html.form.usernameEqualsEmail` is set to false the username is
also part of the self administration.
 
If you are using the standard registration.html template these fields also
configure which fields will be shown in the web browser and which one are
required.

A list of all possible fields are:

```
org.osiam.html.form.fields=formattedName:false\
,familyName:false\
,givenName:false\
,middleName:false\
,givenName:false\
,honorificPrefix:false\
,honorificSuffix:false\
,displayName:false\
,nickName:false\
,profileUrl:false\
,title:false\
,preferredLanguage:false\
,locale:false\
,timezone:false\
,confirmPassword:false\
,email:false\
,phoneNumber:false\
,im:false\
,photo:false\
,formattedAddress:false\
,streetAddress:false\
,locality:false\
,region:false\
,postalCode:false\
,country:false
```

So for example if your configuration looks like this:

    org.osiam.html.form.fields=nickName:true

The string fields `email`, `password` and `nickName` will be shown and all are
required. The field `uerName` will not be shown because the default value of
`org.osiam.html.form.usernameEqualsEmail` is true and so the email will be
taken as userName.

If required is not set, the default is false, except for `email` and `password`.
These are all valid:

    org.osiam.html.form.fields=nickName:true
    org.osiam.html.form.fields=nickName:false
    org.osiam.html.form.fields=nickName

Since you can configure the shown fields in the web browser with this property
you don't have to delete the fields in the `registration.html`.

If you don't have `confirmPassword` as part of the configuration the user only
has to type in his password once to register.

####org.osiam.html.form.extensions

If you wan't to show own extension fields in the self administration you have
to do 3 things (besides from [registering]
(https://github.com/osiam/osiam/blob/master/docs/detailed-reference-installation.md#configuring-scim-extension)
them in the database)

1. You have to add them to the registration.html file

To make this as easy as possible we have added a profile in the htmlfield.html file.
Like this you can add the extension field in a similar way like a standard field.

    <div th:replace="htmlfield :: extensionInput('<urn>', '<field>', '<type>')"></div>

2. Configuration of the extension in the `addon-self-administration.properties`
file by adding them to the property `org.osiam.html.form.extensions` like the
following

    extensions['<urn>'].fields['<field>']:true
    
In this case, this extension field is required, but adding `:true`. Also `false`
and no appending is possible, the default value is `false`.

3. You have to add the placeholder in the registration*.properties` files.

    registration.<urn>.<field>=<placeholder>

Example: `urn:client:extension`

This extension has the fields `age` and `gender`. These fields places under the
`country` field in the `registration.html` file:

```
<div th:replace="htmlfield :: defaultInput('country', 'text', false)"></div>
<div th:replace="htmlfield :: extensionInput('urn:client:extension', 'age', 'text')"></div>
<div th:replace="htmlfield :: extensionInput('urn:client:extension', 'gender', 'text')"></div>
```

Register them in the osiam-self-administration.properties file

```
org.osiam.html.form.extensions=extensions['urn:client:extension'].fields['age']:true\
,extensions['urn:client:extension'].fields['gender']:false
```

Put the placeholder e.g. in the i18n/registration_de_DE.properties file

```
registration.urn\:client\:extension.age=Alter
registration.urn\:client\:extension.gender=Geschlecht
```

Attention! `:` needs to be escaped with `\`

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

# Callback plugin
The callback mechanism provides the possibility to extend the registration process. You can:
* add your own pre-registration check  
* add your own post-registration steps

### Implement a Callback plugin
If you'd like to implement your own plugin, you have to create a little java-project and package it as a jar file. The plugin needs to provide a class that implements the CallbackPlugin interface. We provided a little Plugin-Example-Project that you can find [here](https://github.com/osiam/examples/tree/master/addon-self-administration-plugin).

### Provide and enable the Callback plugin
To enable your plugin you have to configure some properties in _addon-self-administration.properties_ 

```
org.osiam.addon-self-administration.plugin.enabled=true #to enable the pluign
org.osiam.addon-self-administration.plugin.jar.path=<path_to_char> #The absolute path to the jar-file
org.osiam.addon-self-administration.plugin.classname=<full_qualified_class_name> #The full qualified class name
```
