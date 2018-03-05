# Prepare the installable schemata list

# Read each URL and download the IMDF file, and add to the downloadable.json file
# Set homeUrl to the directory that contains the IMDF file.

import requests
import json

listFile = "imdf_list.in"
jsonFile = "downloadable.json"

def appendEntry(jsonOut, url):
    # read url
    # change homeUrl
    # serialize as json
    print(url)
    r = requests.get(url)
    imdf = json.loads(r.content)
    pos = url.rfind("/")
    imdf["homeUrl"] = url[:pos+1]
    jsonList.append(imdf)

jsonList = []

with open(listFile, "r") as file:
    for line in file:
        line = line.strip()
        appendEntry(jsonList, line)

jsonOut = open(jsonFile, "w")
jsonOut.write(json.dumps(jsonList, indent=4, separators=(',', ': ')))
