package org.operaton.spin.groovy.json.tree

node = S(input, "application/json")

property1 = node.prop("active")

value1 = property1.boolValue()