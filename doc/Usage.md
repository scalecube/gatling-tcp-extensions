#Usage

Tcp support is an extension to the HTTP DSL, whose entry point is the ```scala tcp(requestName: Expression[String]) ``` method.

Tcp protocol is very different from the HTTP one as the communication is 2 ways: both client-to-server and server-to-client,
so the model is different from the HTTP request/response pair.

##Connect
The first thing is to create new tcp connection:

    connect()
For example:
``` scala
exec(tcp("Connect").connect())
```

##Disconnect
When you’re done with a connection, you can close it:

    disconnect()
For example:
``` scala
exec(tcp("Close").disconnect)
```

##Send a Message
Only text messages is supported now:

    sendText(text: Expression[String])

For example:
```scala
exec(tcp("Message")
  .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))
```

##Server Messages: Checks

Dealing with incoming messages from the server is done with checks, passed with the usual check() method.

Gatling currently only support one check at a time per WebSocket.
Set a Check

Checks can be set when sending a message:
``` scala
exec(tcp("Send").sendText("hello").check(myCheck))
```
If a check was already registered on the tcp at this time, it’s considered as failed and replaced with the new one.

##Build a Check
Now, to the matter at heart, how to build a tcp check.

Only blocking checks are supported now, so main flow is blocked until the checks completes or not

tcpCheck creates a blocking check: the main HTTP flow is blocked until the check completes.

###Step 1: Set the Timeout

    within(timeout: FiniteDuration)

###Step 2: Exit condition
TBD (Not implemented yet. should be skipped)

until(count: Int): the check will succeed as soon as Gatling has received the expected count of matching messages

expect(count: Int): Gatling will wait until the timeout and the check will succeed if it has received the expected count of matching messages

expect(range: Range): same as above, but use a range instead of a single expected count

###Step 3: Matching condition

Tcp checks support  only regex check the same as for HTTP bodies
```scala
regex(expression: Expression[String]): use a regular expression
```

See HTTP counterparts for more details.

###Step 5: Saving (optional)

Just like regular HTTP checks, one can use checks for saving data into the virtual user’s session.

Here are an example:

``` scala
exec(
  tcp("Send Message")
    .sendText("hello, I'm Stephane")
    .check(tcpCheck.within(30 seconds).regex("hello (.*)").saveAs("name"))
)
```

##Configuration

Tcp extensions protocol configuration introduce new entry point tcp:

address(hostname: String): hostname where to connection should be created

port(port: Int): similar to standard baseURLs for HTTP, serves as round-robin roots that will be prepended to all relative WebSocket urls

### Delimiters
Tcp is stream based protocol so need to distinguish messages one from each other.
Now three types of delimiters is supported:
1. lengthBased(length: Int) - length based delimiter - the length of each message is prepended to message, and has size length in bytes.
2. delimiterBased(delimiters : String, strip : Boolean) - messages delimit by delimeters in the end of message.
3. protobufVarint - prepend protobuf varint length to the message.

##Example

```scala
  val tcpConfig = tcp.address("127.0.0.1").port(6000).lengthBased(4)
  val scn = scenario("Tcp")
    .exec(tcp("Connect").connect())
    .pause(1)
    .repeat(2, "i") {
       exec(tcp("Say Hello Tcp")
      .sendText( ""Hello TCP""")
       .check(tcpCheck.within(5 seconds).regex( """Hello(.+?)""").saveAs("name"))
       ).pause(1)
  }
    .exec(tcp("disconnect").disconnect())

  setUp(scn.inject(atOnceUsers(5))).protocols(tcpConfig)
```
