#!/usr/bin/python
import requests
import re
import threading

playSessionCookieKey = 'PLAY_SESSION'

def extractCsrfToken(body):
    csrfToken = re.search(r'[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}', body).group()
    return csrfToken

def getRequestString(req):
    result = '{} HTTP/1.1\n{}\n\n{}'.format(req.method + ' ' + req.url, '\n'.join('{}: {}'.format(k, v) for k, v in req.headers.items()), req.body)
    return result

def prepareRequest():
    # get csrf token
    response = requests.get('http://localhost:9000/login')
    sessionCookie = response.cookies.get(playSessionCookieKey)
    csrfToken = extractCsrfToken(response.text)
    cookies = {playSessionCookieKey: sessionCookie}
    payload = {'credentials': 'admin', 'password': 'admin1', 'csrfToken': csrfToken}

    # get login cookie
    response = requests.post('http://localhost:9000/login', data=payload, cookies=response.cookies, allow_redirects=False)
    cookies = response.cookies

    response = requests.get('http://localhost:9000/fileupload')
    files = {'file': open('/Users/juliuszint/Desktop/upload.txt', 'rb')}
    headers = {'Host': 'localhost:9000'}
    payload = {'csrfToken': csrfToken, 'filename': 'frompython', 'comment': 'hi'}
    request = requests.Request('POST', 'http://localhost:9000/fileupload', data=payload, files=files, cookies=cookies, headers=headers)
    #requestString = getRequestString(request.prepare())
    return request

threads = []
thread_count = 10

def worker(preparedRequest):
    return

for i in range(thread_count):
    t = threading.Thread(target=worker, args=(prepareRequest(), ))
    threads.append(t)