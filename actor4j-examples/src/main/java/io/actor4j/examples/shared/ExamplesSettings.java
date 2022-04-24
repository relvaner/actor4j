package io.actor4j.examples.shared;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystemFactory;

public final class ExamplesSettings {
	public static ActorSystemFactory factory() {
		return ActorRuntime.factory();
	}
}
