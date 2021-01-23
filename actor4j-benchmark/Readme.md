## Actor4j Benchmark ##

This is the repository for Actor4j benchmark.

>Aim of this project was to enhance the performance in message passing. As a reference implementation `Akka` was used. Results of the research shown that intra-thread-communication is much better than inter-thread-communication. You can group actors, so they are bound to the same thread, for instance. Message queues of the actors are outsourced to the thread. The four principles of reactive manifesto and the four semantic properties of actor systems have been applied.

For further information on `Actor4j`, see the following more complete [documentation](https://github.com/relvaner/actor4j-core).

### Benchmarks (Excerpt) ###

| Framework | Benchmark | Description |
| :---: | :---: | :---: |
| Actor4j | ping.pong.grouped | This benchmark shows inter-communication between actors on different threads. Here, actors are bundled as a distributed pair and communicate with each other. Synchronization is necessary [[1](#1)]. |
| Actor4j | ping.pong.bulk | This benchmark shows inter- and intra-communication between the actors (estimated distribution N:1). Here, actors are bundled as a (distributed) pair and communicate with each other. It is based on the benchmark for Akka or Akka.NET, which was used to advertise (50 million msg/s) the resulting message throughput. [[1](#1)]. |
| Actor4j | ring.nfold | This benchmark shows intra-communcation between actors on the same thread. Here, actors are bundled in groups and communicate in a ring structure with each other. No synchronization is needed [[1](#1)]. |
| Akka | ping.pong | - |
| Akka | ping.pong.bulk | - |
| Akka | ring.nfold | - |
| JADE | ping.pong | - |
| JADE | ping.pong.bulk | - |
| JADE | ring.nfold | - |

Command Line Tool - Example for Actor4j:
>java -jar benchmark.jar -class ping.pong.grouped -threads 16 -factor 1 -actors 100 -warmup 10 -duration 60000

Command Line Tool - Example for Akka:
>java -jar benchmark.jar -akka -class ping.pong -threads 16 -factor 1 -actors 100 -warmup 10 -duration 60000

## References ##
[1]<a name="1"/> D. A. Bauer and J. Mäkiö, “Actor4j: A Software Framework for the Actor Model Focusing on the Optimization of Message Passing,” AICT 2018: The Fourteenth Advanced International Conference on Telecommunications, IARIA, Barcelona, Spain 2018, pp. 125-134, [Online]. Available from: http://www.thinkmind.org/download.php?articleid=aict_2018_8_10_10087

Page to be updated 09/12/2020

