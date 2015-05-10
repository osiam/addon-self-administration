## Database setup

For the registration you need to add some extension fields into the database otherwise it will not work.

The extension is configured with it's own namespace and will not conflict user defined extensions.

Start the database commandline:

`$ sudo -u postgres psql`

Now insert it as user osiam by calling

`$ psql -f ./sql/registration_extension.sql -U osiam`

while being in the directory where you unpacked the sources.