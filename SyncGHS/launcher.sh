#!/bin/bash

# Change this to your netid
netid=nxp161330

# Root directory of your project
PROJDIR=/home/010/n/nx/nxp161330/project1

# Directory where the config file is located on your local system
CONFIGLOCAL1=/Users/nilesh/git/SynGHS/SyncGHS/config.txt

# Directory where the config file is located on your local system
CONFIGLOCAL=/home/010/n/nx/nxp161330/project1/config.txt

# Directory your java classes are in
BINDIR=/home/010/n/nx/nxp161330/project1

# Your main project class
PROG=/home/010/n/nx/nxp161330/project1/ghs.jar

n=0

cat $CONFIGLOCAL1 | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
    while [[ $n -lt $i ]]
    do
    	read line
    	p=$( echo $line | awk '{ print $1 }' )
        host=$( echo $line | awk '{ print $2 }' )
	
	osascript -e 'tell app "Terminal"
        do script "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no '$netid@$host' java -jar '$PROG' '$p' '$CONFIGLOCAL'; '$SHELL'"
        end tell'

        n=$(( n + 1 ))
    done
)
