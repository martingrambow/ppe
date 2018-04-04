# Benchmarks encryption via systemcall in java.
#
# Usage:
#
# python benchmarkSystemcall.py key value
# example: python benchmarkSystemcall.py Friday value
#

import sys
from pyope.ope import OPE
from pyope.ope import ValueRange

# store key
key = str(sys.argv[1])
inrange = ValueRange(0, 2**34-1)
outrange = ValueRange(0, 2**50-1)
cipher = OPE(key, inrange, outrange)

val = int(sys.argv[2]);
enc = cipher.encrypt(val)
dec = cipher.decrypt(enc)
answer = '(' + str(enc) + ',' + str(dec) + ')'
print answer
