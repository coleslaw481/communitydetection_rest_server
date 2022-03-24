#!/usr/bin/env python

import os
import sys
import json
import time
import requests
import ndex2

REST_ENDPOINT = 'http://localhost:8081/cd/cd/v1'

HEADERS = {'Content-Type': 'application/json',
           'Accept': 'application/json'}

if len(sys.argv) != 2:
    print('Usage: ' + sys.argv[0] + ' <path to write CX file>\n\n')
    sys.exit(1)

# create nice cx object
print('creating nice cx object')
nice_cx = ndex2.create_nice_cx_from_server('www.ndexbio.org', uuid='67c3b75d-6191-11e5-8ac5-06603eb7f303')



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
    with open(sys.argv[1], 'wb') as f:
       for chunk in res.iter_content(1024):
           f.write(chunk)
    print('Output written to: ' + sys.argv[1])
    sys.exit(0)
else:
    print('Non 200 status code received: ' + str(res.status_code))
    sys.exit(1)
    


