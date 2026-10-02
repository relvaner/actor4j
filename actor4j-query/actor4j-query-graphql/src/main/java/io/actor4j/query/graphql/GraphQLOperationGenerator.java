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
package io.actor4j.query.graphql;

import graphql.schema.*;

import java.util.Locale;
import java.util.stream.Collectors;

public final class GraphQLOperationGenerator {
	public static String build(GraphQLSchema schema, String operationType, String operationName) {
		return build(schema, operationType, operationName, null);
	}

	public static String build(GraphQLSchema schema, String operationType, String operationName, String selection) {
		if (operationType == null || operationName == null)
			return null;

		String type = operationType.toLowerCase(Locale.ROOT);

		GraphQLObjectType rootType = switch (type) {
			case "query" -> schema.getQueryType();
			case "mutation" -> schema.getMutationType();
			case "subscription" -> schema.getSubscriptionType();
			default -> null;
		};

		GraphQLFieldDefinition field = rootType != null ? rootType.getFieldDefinition(operationName) : null;
		if (field == null)
			return null;

		String fieldSelection = (selection != null && !selection.isBlank())
			? " { " + selection.trim() + " }"
			: selection(field.getType());

		return buildOperation(field, type, fieldSelection);
	}

	protected static String buildOperation(GraphQLFieldDefinition field, String operationType, String fieldSelection) {
		String varDefs = field.getArguments().stream()
			.map(a -> "$" + a.getName() + ": " + GraphQLTypeUtil.simplePrint(variableType(a)))
			.collect(Collectors.joining(", "));

		String args = field.getArguments().stream()
			.map(a -> a.getName() + ": $" + a.getName())
			.collect(Collectors.joining(", "));

		return operationType + " " + field.getName()
			+ (varDefs.isEmpty() ? "" : "(" + varDefs + ")")
			+ " { " + field.getName()
			+ (args.isEmpty() ? "" : "(" + args + ")")
			+ fieldSelection
			+ " }";
	}

	protected static GraphQLType variableType(GraphQLArgument argument) {
		GraphQLInputType type = argument.getType();
		if (argument.hasSetDefaultValue() && GraphQLTypeUtil.isNonNull(type))
			return GraphQLTypeUtil.unwrapNonNull(type);
		return type;
	}

	protected static String selection(GraphQLOutputType type) {
		GraphQLType unwrapped = GraphQLTypeUtil.unwrapAll(type);

		if (unwrapped instanceof GraphQLUnionType)
			return " { __typename }";

		if (!(unwrapped instanceof GraphQLFieldsContainer container))
			return "";

		String fields = container.getFieldDefinitions().stream()
			.filter(f -> !f.isDeprecated())
			.filter(f -> f.getArguments().isEmpty())
			.filter(f -> {
				GraphQLType t = GraphQLTypeUtil.unwrapAll(f.getType());
				return t instanceof GraphQLScalarType || t instanceof GraphQLEnumType;
			})
			.map(GraphQLFieldDefinition::getName)
			.collect(Collectors.joining(" "));

		return " { " + (fields.isEmpty() ? "__typename" : fields) + " }";
	}
}