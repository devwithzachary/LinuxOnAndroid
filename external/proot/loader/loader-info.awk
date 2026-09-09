# Note: This file is included only for targets which have pokedata workaround
function parse_hex(h,   n, i, c, v) {
	sub(/^0[xX]/, "", h)
	h = tolower(h)
	n = 0
	for (i = 1; i <= length(h); i++) {
		c = substr(h, i, 1)
		v = index("0123456789abcdef", c) - 1
		if (v < 0) break
		n = n * 16 + v
	}
	return n
}
/pokedata_workaround/{pokedata_workaround=parse_hex($2)}
/_start/{start=parse_hex($2)}
END {
	print "#include <unistd.h>"
	print "const ssize_t offset_to_pokedata_workaround=" (pokedata_workaround-start) ";"
}

