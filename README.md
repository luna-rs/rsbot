jBot
====
An open source API that enables developers to create and manage their own [local Runescape bots](http://www.rune-server.org/runescape-development/rs2-server/show-off/561994-serversided-bots.html). It's designed to be easy-to-use and to allow the player to have as much control over the functionality of the bots as possible. All of the code is extensively documented so the user can fully understand what's going on behind the scenes.

jBot currently runs on Java 8, so users will need to be developing using Java 8 in order to use this API.

Table of contents
-------

- [Bots](#bots)
- [IO] (#io)
- [Exception Handling](#exception-handling)
- [Providers](#providers)
- [Task Management](#task-management)
- [Releases] (#releases)

### Bots
-------
The two main classes that the user will be utilizing are ```JBot``` and ```JBotGroup```. A ```JBot``` instance holds all information related to a bot (io, credentials, bot group, state, etc.) and every ```JBot``` belongs to a ```JBotGroup``` instance. A ```JBotGroup``` instance holds all information related to how messages and exceptions will be handled, and allocates the resources required for bots to be able to login and perform actions. Those specific resources will be elaborated on in later sections.

</br>
A new bot and bot group with default settings should be started like so

```
JBotGroup mainGroup = new JBotGroupBuilder().build();

...

JBot myBot = mainGroup.add("myusername", "mypassword");
```

Alternatively, one may need to configure a bot and bot group with personalized settings

```
JBotGroup mainGroup = new JBotGroupBuilder().exceptionHandler(new MyCustomExceptionHandler())
.rsaKey(new JBotRsaKey(...)).build();

...

JBot myBot = mainGroup.add("myusername", "mypassword");
```


### IO
-------
All IO for bots is done using asynchronous NIO. In other words, all IO functions will return instantly instead of blocking until completion. All IO events are handled by the ```JBotReactor```. If for some reason the reactor's thread is interrupted it will throw an ```IllegalStateException```, log out all bots, and then release all resources that it had previously acquired when it was active. This renders the underyling ```JBotGroup``` attached to this reactor, useless.

</br>
Outgoing messages are controlled by the ```JBotMessageWriter```. If a custom message not already supplied needs to be written then users should extend that class. Outgoing messages are sent like so

```
JBot myBot = ...;

myBot.write(new Write317TalkMessage("I'm talking!"));
myBot.write(new Write317ButtonMessage(4553));

myBot.write(new WriteMyCustomMessage(arg1, arg2, "my own impl of JBotMessageWriter!"));
```

</br>
Incoming messages are controlled by the ```JBotMessageReader``` ...

### Exception Handling
-------
stuff here


### Providers
-------
stuff here


### Task Management
-------
stuff here


### Releases
-------
The current stable version is (x.x.x).

(downloads of all previous .JARs with sources)
