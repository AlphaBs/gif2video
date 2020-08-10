import os
from os import listdir
from os.path import isfile, join

def calc(org, cps):
    return (1 - cps / org) * 100

target_path = './'
files = [f for f in listdir(target_path) if isfile(join(target_path, f))]

for f in files:
    name, ext = os.path.splitext(f)
    
    if ext != '.gif':
        continue

    mp4path = name + '.mp4'
    
    if not os.path.exists(mp4path):
        continue
    
    print(name + " ", end='')

    gifsize = os.stat(f).st_size
    mp4size = os.stat(mp4path).st_size
    ratio = calc(gifsize, mp4size)

    print(ratio)

print("end")
input()
