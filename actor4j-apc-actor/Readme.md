## Example ##

Message exchange via asynchronous method call (a message is internally sent to the underlying actor). Similar implementation to Microsoft Azure Service Fabric Reliable Actors, Microsoft Orleans [1] or ActorFoundry.

```java
public interface Greeter {
	void sayGreeting();
	void sayGreeting(String name);
	Future<Integer> task(Integer number);
}

public class GreeterImpl extends APCObject implements Greeter {
	@Override
	public void sayGreeting() {
		logger().debug(String.format("sayGreeting: Hello Developer!"));
	}
    
	@Override
	public void sayGreeting(String name) {
		logger().debug(String.format("sayGreeting: Hello %s", name));
	}
    
	@Override
	public Future<Integer> task(Integer number) {
		return handleFuture((f) -> f.complete(number+1));
	}
}
	
APCActorSystem system = new APCActorSystem();
APCActorRef<Greeter> ref = system.addAPCActor(Greeter.class, new GreeterImpl());

ref.tell().sayGreeting();
ref.tell().sayGreeting("David");

// We must start our actor system first, because the next call is a blocking future get().
system.start();
	
try {
	logger().debug(String.format("task: Result is %d", ref.tell().task(41).get()));
} catch (InterruptedException | ExecutionException e) {
	e.printStackTrace();
}
```

## References ##

[1] https://github.com/dotnet/orleans
