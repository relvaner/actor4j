/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.verification.runtime;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jgrapht.Graph;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.runtime.DefaultActorSystemImpl;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.verification.ActorVerification;
import io.actor4j.verification.ActorVerificationEdge;
import io.actor4j.verification.ActorVerificationEdgeTuple;
import io.actor4j.verification.ActorVerificationSM;
import io.actor4j.verification.ActorVerificationState;
import io.actor4j.verification.ConformanceResult;
import io.actor4j.verification.ActorVerificationUtils;
import io.actor4j.verification.ActorVerificator;
import io.actor4j.verification.config.ActorVerificationConfig;

public class VerificatorActorSystemImpl extends DefaultActorSystemImpl implements ActorVerificator {
	// message observed at runtime (sender state only if the sender implements ActorVerificationState)
	protected record Observed(Actor sender, String senderState, int tag, List<Actor> receivers, String alias) {
	}

	protected final Queue<Observed> observed = new ConcurrentLinkedQueue<>();

	public VerificatorActorSystemImpl() {
		this(null);
	}

	public VerificatorActorSystemImpl(ActorVerificationConfig config) {
		super(config);

		messageDispatcher = new VerificatorActorMessageDispatcher(this);
	}
	
	@Override
	public boolean setConfig(ActorVerificationConfig config) {
		return super.setConfig(config);
	}
	
	@Override
	public void verify(Consumer<ActorVerificationSM> consumer) {
		if (consumer!=null) {
			Function<InternalActorCell, Boolean> verifyAction = (cell) -> {
				if (cell.getActor() instanceof ActorVerification)
					consumer.accept(((ActorVerification)cell.getActor()).verify());
				return false;
			};
			internal_iterateCell((InternalActorCell)USER_ID, verifyAction);
		}				
	}
	
	@Override
	public void verifyAll(Consumer<ActorVerificationSM> consumer, Consumer<Graph<String, ActorVerificationEdge>> consumerAll) {
		List<ActorVerificationSM> list = new ArrayList<>();
		verify(list::add);
		ActorVerificationUtils.uniqueNames(list);

		if (consumer!=null)
			list.forEach(consumer);
		if (consumerAll!=null)
			consumerAll.accept(ActorVerificationUtils.overallGraph(list));
	}

	// called by the dispatcher in the thread of the sender
	public void record(ActorMessage<?> message, String alias) {
		if (message.tag()<0) // internal messages (stop, restart, ...)
			return;

		Actor sender = actorOf(message.source());
		List<Actor> receivers = new ArrayList<>();
		if (message.dest()!=null)
			receivers.add(actorOf(message.dest()));
		else if (alias!=null)
			for (ActorId id : getActorsFromAlias(alias))
				receivers.add(actorOf(id));
		receivers.removeIf((actor) -> actor==null);

		if (sender instanceof ActorVerification || receivers.stream().anyMatch((actor) -> actor instanceof ActorVerification))
			observed.add(new Observed(sender, sender instanceof ActorVerificationState state ? state.state() : null, message.tag(), receivers, alias));
	}

	protected Actor actorOf(ActorId id) {
		return id instanceof InternalActorCell cell ? cell.getActor() : null;
	}

	@Override
	public void clearObserved() {
		observed.clear();
	}

	@Override
	public ConformanceResult conformance() {
		return conformance(Map.of());
	}

	@Override
	public ConformanceResult conformance(Map<Integer, String> eventNames) {
		Map<Actor, ActorVerificationSM> models = new IdentityHashMap<>();
		Function<Actor, ActorVerificationSM> model = (actor) -> models.computeIfAbsent(actor, (a) -> ((ActorVerification)a).verify());
		Function<Integer, String> event = (tag) -> eventNames.getOrDefault(tag, String.valueOf(tag));

		LinkedHashSet<String> violations = new LinkedHashSet<>();
		int count = 0;
		for (Observed o : observed) {
			count++;
			if (o.sender() instanceof ActorVerification) {
				ActorVerificationSM sm = model.apply(o.sender());
				if (!sends(sm, o))
					violations.add(name(o.sender())+" sent "+event.apply(o.tag())+" to "+receiver(o)
						+(o.senderState()!=null ? " in state "+o.senderState() : "")+", not in the model");
			}
			for (Actor receiver : o.receivers())
				if (receiver instanceof ActorVerification && !receives(model.apply(receiver), o.tag()))
					violations.add(name(receiver)+" received "+event.apply(o.tag())
						+(o.sender()!=null ? " from "+name(o.sender()) : "")+", not in the model");
		}

		return new ConformanceResult(count, new ArrayList<>(violations));
	}

	// the model sends the event to the receiver (by alias or name), from or to the current state if known
	protected boolean sends(ActorVerificationSM sm, Observed o) {
		for (ActorVerificationEdge edge : sm.getGraph().edgeSet()) {
			if (edge.getTuples()==null)
				continue;
			if (o.senderState()!=null) {
				String state = sm.getPrefix()+o.senderState();
				if (!state.equals(sm.getGraph().getEdgeSource(edge)) && !state.equals(sm.getGraph().getEdgeTarget(edge)))
					continue;
			}
			for (ActorVerificationEdgeTuple tuple : edge.getTuples())
				if (tuple.getEvents().contains(o.tag()) && addressed(tuple, o))
					return true;
		}

		return false;
	}

	protected boolean addressed(ActorVerificationEdgeTuple tuple, Observed o) {
		if (o.alias()!=null && tuple.getAliases().contains(o.alias()))
			return true;
		for (Actor receiver : o.receivers())
			if (receiver.getName()!=null && tuple.getAliases().contains(receiver.getName()))
				return true;

		return false;
	}

	protected boolean receives(ActorVerificationSM sm, int tag) {
		for (ActorVerificationEdge edge : sm.getGraph().edgeSet())
			if (edge.getEvents()!=null && edge.getEvents().contains(tag))
				return true;

		return false;
	}

	protected String name(Actor actor) {
		return actor.getName()!=null ? actor.getName() : actor.getClass().getSimpleName();
	}

	protected String receiver(Observed o) {
		if (o.alias()!=null)
			return o.alias();
		return o.receivers().isEmpty() ? "?" : name(o.receivers().get(0));
	}
}
