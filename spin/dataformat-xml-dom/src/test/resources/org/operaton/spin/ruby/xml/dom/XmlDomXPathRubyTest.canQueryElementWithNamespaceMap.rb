map = {
  "a" => "http://operaton.com"
}

$query = S($input).xPath($expression).ns(map)
