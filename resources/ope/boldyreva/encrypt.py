# Encrypts given integer value with given key.
#
# Usage:
#
# python encrypt.py key integerValue
# example: python encrypt.py Friday 1337
#

import sys
from pyope.ope import OPE
from pyope.ope import ValueRange
inrange = ValueRange(0, 2**34-1)
outrange = ValueRange(0, 2**50-1)
cipher = OPE(str(sys.argv[1]), inrange, outrange)
print(cipher.encrypt(int(sys.argv[2])))

