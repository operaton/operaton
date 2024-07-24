package org.operaton.spin.groovy.json.tree

jsonNode = S(input, "application/json");

jsonNode.jsonPath('$.order').boolValue();