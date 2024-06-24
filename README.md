# spring-amqp-demo

This project aims to demonstrate a RabbitMQ broker losing messages when said broker is restarted while receiving messages.

## Usage
First, we need to hava a running RabbitMQ instance. For simplicity's sake we will use docker:

```shell
docker run -d --hostname my-rabbit --name my-rabbit -v $(pwd)/rabbit-data:/var/lib/rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13.3-management
```

We can now access the management UI via http://localhost:15672 using `guest` / `guest` as credentials.  

For our next step we need to launch this application. Without using an IDE the easiest way would be:

```shell
./mvnw spring-boot:run
```

Now, when we access the URL http://localhost:8080 we will see a new queue `myqueue` in the RabbitMQ UI, which will gradually fill with 500,000 messages (as seen in the Controller). Depending on your performance you may override the message count with the `count` parameter: http://localhost:8080/?count=200000

While the queue is filling up, we restart the broker:

```shell
docker restart my-rabbit
```

While it's restarting the application will now output some error logs, which is somewhat expected (although I assumed that the retry configuration "just works").

After our loop is finished we get a log message (which is also the HTTP Response you will see in your browser):

`Sent 187380 messages in total. Failed count: 12620`

This adds up nicely to my, in this case, 200,000 messages.

Now we can compare the 187380 messages with the number shown in the UI, which, in my case, only shows 187379 messages.

Do note that losing one or some messages doesn't always happen. Sometimes they all go through, which makes testing this quite annoying. Sometimes it only happens after ten successes in a row. 