# WordCount
WordCount README

Welcome to WordCount Component

 WordCount is used read file (xml, txt, doc), save word in MongoDB and count Most common and least common word from MongoDB,
That will be most count and least count will be output for the WordCount.

Software Requirement
------------------------

1) Install jdk1.8
2) Install mongoDB server 3.4,set class path and required setup to start MongoDB server


Execution for WordCount
-------------------------

1) Start MongoDB server using 'mongod' command
2) Execute wordcount executable jar using following command 

you can execute jar followed by 2 way, use command for execution

2.1) java -jar mongoDBTest-0.0.1-SNAPSHOT-jar-with-dependencies fileName ipaddress:port number
    1) Instruction: - File name can be text, doc or xml
2.2) java -jar mongoDBTest-0.0.1-SNAPSHOT-jar-with-dependencies
    1) After executing jar we can provide File Name, IpAddress and Port number as input

MongoDB verification for result
--------------------------------

1) 'wordcount' as a collection we used in MongoDB
2) File name as a document will create in MongoDB collection

java -jar mongoDBTest-0.0.1-SNAPSHOT-jar-with-dependencies test.txt 192.168.0:27017
'text' will be document name in MongoDB collection if you will use same name file, file will use same file 'text'

Command for MongoDB
----------------------
'use wordcount' command to switched to db wordcount
command for Most common word
-------------------------------
db.text.find().sort({word : -1}).limit(1)
db.bank.find({word : NumberOfValueOfWord})})

command for Least common word
-------------------------------
db.text.find().sort({word : +1}).limit(1)
db.bank.find({word : NumberOfValueOfWord})












