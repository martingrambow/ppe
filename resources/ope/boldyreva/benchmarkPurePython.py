# Benchmarks en-/decrypion in pure python
#
# Usage:
#
# python benchmarkPurePython.py key startvalue maxvalue
# example: python benchmarkPurePython.py Friday 0 10000
#

import sys
from pyope.ope import OPE
from pyope.ope import ValueRange
from time import *

inrange = ValueRange(0, 2**34-1)
outrange = ValueRange(0, 2**50-1)
cipher = OPE(str(sys.argv[1]), inrange, outrange)

t1 = clock()

i = int(sys.argv[2])
maxvalue = int (sys.argv[3])
while (i < maxvalue):
  enc = cipher.encrypt(i)
  dec = cipher.decrypt(enc)
  #print str(i) + ':(' + str(enc) + ', ' + str(dec) + ')'
  i = i + 1

t2 = clock()

print 'duration = ' + str((t2-t1)*1000) + 'ms'
