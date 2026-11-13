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
package io.actor4j.verification;

import java.util.ArrayList;
import java.util.List;

// verifies actors without an actor system: collects their models, makes names unique and interconnects them
public final class ActorVerifier {
	private ActorVerifier() {
	}

	public static VerificationResult verify(ActorVerification... actors) {
		return verify(List.of(actors));
	}

	public static VerificationResult verify(List<? extends ActorVerification> actors) {
		List<ActorVerificationSM> list = new ArrayList<>();
		for (ActorVerification actor : actors)
			list.add(actor.verify());

		return verifyModels(list);
	}

	public static VerificationResult verifyModels(List<ActorVerificationSM> models) {
		List<ActorVerificationSM> list = new ArrayList<>(models);
		ActorVerificationUtils.uniqueNames(list);

		return new VerificationResult(list, ActorVerificationUtils.overallGraph(list));
	}
}
