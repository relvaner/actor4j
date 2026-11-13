/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.actor4j.testing.features;

import java.util.ArrayList;
import java.util.List;

import io.actor4j.bdd.Scenario;
import io.actor4j.bdd.Step;
import io.actor4j.bdd.Story;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.testing.ActorTest;

public class ResponderActor extends Actor implements ActorTest {
	protected final int replies;
	protected final long delay;
	protected final List<List<Step>> stories;

	public ResponderActor(int replies) {
		this(replies, 0);
	}

	public ResponderActor(int replies, long delay) {
		super("responder");
		this.replies = replies;
		this.delay = delay;
		stories = new ArrayList<>();
		stories.add(new ArrayList<>());
	}

	public ResponderActor step(Step step) {
		stories.get(stories.size()-1).add(step);
		return this;
	}

	public ResponderActor nextStory() {
		stories.add(new ArrayList<>());
		return this;
	}

	public void request() {
		send(ActorMessage.create("request", 0, getSystem().SYSTEM_ID(), self()));
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (delay>0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		for (int i=0; i<replies; i++)
			tell("reply"+i, 0, message.source());
	}

	@Override
	public List<Story> test() {
		List<Story> result = new ArrayList<>();
		for (List<Step> steps : stories) {
			Story story = new Story();
			Scenario scenario = story.scenario();
			for (Step step : steps)
				scenario.and(step);
			result.add(story);
		}

		return result;
	}
}
