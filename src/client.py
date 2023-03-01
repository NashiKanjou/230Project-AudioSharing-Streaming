import requests

url = 'http://localhost:8080/'
myobj = {'title': 'song1'}
title = 'songtitle'

y = requests.get(url + title)
#y = requests.get(url, json = myobj)
#y = requests.post(url, json = myobj)

print("info received was: " + str(y.text))
