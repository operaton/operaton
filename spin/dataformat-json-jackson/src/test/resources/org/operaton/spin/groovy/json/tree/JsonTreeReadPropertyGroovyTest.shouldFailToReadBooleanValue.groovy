package org.operaton.spin.groovy.json.tree

node = S(input, "application/json");

property1 = node.prop("order");

value1 = property1.boolValue();