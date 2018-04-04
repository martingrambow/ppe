# Decrypts given integer value with given key.
#
# Usage:
#
# python decrypt.py key integerValue
# example: python decrypt.py Friday 87072464
#

import sys
from pyope.ope import OPE
from pyope.ope import ValueRange
inrange = ValueRange(0, 2**34-1)
outrange = ValueRange(0, 2**50-1)
cipher = OPE(str(sys.argv[1]), inrange, outrange)
print(cipher.decrypt(int(sys.argv[2])))


