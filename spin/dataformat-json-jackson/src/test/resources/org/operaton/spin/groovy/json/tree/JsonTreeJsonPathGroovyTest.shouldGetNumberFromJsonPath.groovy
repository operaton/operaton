package org.operaton.spin.groovy.json.tree

jsonNode = S(input, "application/json");

numberValue = jsonNode.jsonPath('$.orderDetails.price').numberValue();