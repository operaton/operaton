package org.operaton.spin.groovy.json.tree

jsonNode = S(input, "application/json");

node = jsonNode.jsonPath('$.customers[0]').element();