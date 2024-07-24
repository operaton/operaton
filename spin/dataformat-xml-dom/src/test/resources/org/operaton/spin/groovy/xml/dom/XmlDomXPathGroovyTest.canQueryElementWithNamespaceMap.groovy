package org.operaton.spin.groovy.xml.dom
def map = [
  a:"http://operaton.com"
]

query = S(input).xPath(expression).ns(map)
