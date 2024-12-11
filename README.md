Documenation is a little rough around the edges, but hopefully that will improve as we go.



### Terror zone notification setup

To setup the terror zone notificaitons you need to follow the steps below.

1) Create a role for any unique notifications you want your users to recieve. These can be as granular as you want. However recommendation is to create a server role for each existing terror zone.

Eg. `TZ:Blood Moor`

2) Run the command `/tz` in the server channel you want to get terror zone notifications. You will then be prompted to assign each of the created roles to each possible terrorzone. Rememeber to assign the roles using the `@TZ:Blood Moor` role mention.

3) Create a channel for users to `#get-roles` this can be an existing channel, or it can be a new `#get-tz-roles` channel. In this channel run the `/tzrolebutton` command. This will create a prompt users will see when wanting to join roles. Some possible suggested text.

message: `Ready to setup Terror zone Notifications?`
Button: `Click here to start setting up Notifications`
