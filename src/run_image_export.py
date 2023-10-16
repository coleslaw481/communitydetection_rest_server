#!/usr/bin/env python

import os
import sys
import json
import time
import requests
import ndex2

#REST_ENDPOINT = 'http://localhost:8081/cd/cd/v1'
REST_ENDPOINT = 'http://cd.ndexbio.org/cd/communitydetection/v1'
HEADERS = {'Content-Type': 'application/json',
           'Accept': 'application/json'}

if len(sys.argv) != 3:
    print('Usage: ' + sys.argv[0] + ' <CX file or UUID of network on www.ndexbio.org> <output png file>\n\n')
    sys.exit(1)

# create nice cx object
if os.path.isfile(sys.argv[1]):
    print('Loading Network ' + str(sys.argv[1]))
    nice_cx = ndex2.create_nice_cx_from_file(sys.argv[1])
else:
    print('Loading Network with UUID ' + str(sys.argv[1]) + ' from NDEx')
    nice_cx = ndex2.create_nice_cx_from_server('www.ndexbio.org', uuid=str(sys.argv[1]))



# pass CX in dict format under 'data' key of dict using to_cx() method 
# in nice cx object
print('making request to run image export')
res = requests.post(REST_ENDPOINT,
                    headers=HEADERS,
                    json={'algorithm': 'cytojsimageexport',
                          'customParameters': { '--width': '2048', '--height': '2048' },
                          'data': nice_cx.to_cx()},
                    timeout=30)

# if successful status code is 202
if res.status_code != 202:
    raise Exception('Error submitting image export task ' + str(res.status_code) + ' : ' + str(res.text))

task_id = res.json()['id']

print('Task id is: ' + str(task_id))

# need to make GET request with task id to check job status
res = requests.get(REST_ENDPOINT + '/raw/' + str(task_id))

if res.status_code == 200:
    with open(sys.argv[2], 'wb') as f:
       for chunk in res.iter_content(1024):
           f.write(chunk)
    print('Output written to: ' + sys.argv[2])
    sys.exit(0)
else:
    print('Non 200 status code received: ' + str(res.status_code))
    sys.exit(1)
    


